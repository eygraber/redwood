/*
 * Copyright (C) 2021 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.redwood.schema.parser

import app.cash.redwood.schema.Children as ChildrenAnnotation
import app.cash.redwood.schema.Default as DefaultAnnotation
import app.cash.redwood.schema.LayoutModifier as LayoutModifierAnnotation
import app.cash.redwood.schema.Property as PropertyAnnotation
import app.cash.redwood.schema.Schema as SchemaAnnotation
import app.cash.redwood.schema.Widget as WidgetAnnotation
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

private val childrenType = Function::class.starProjectedType
private val eventType = Function::class.starProjectedType
private val optionalEventType = eventType.withNullability(true)

private const val maxSchemaTag = 4_000U
private const val maxMemberTag = 1_000_000U

private val KClass<*>.schemaAnnotation: SchemaAnnotation get() {
  return requireNotNull(findAnnotation()) { "Schema $qualifiedName missing @Schema annotation" }
}

public fun parseSchema(schemaType: KClass<*>, tag: UInt = 0U): Schema {
  val schemaAnnotation = schemaType.schemaAnnotation
  require(tag in 0U..maxSchemaTag) { "Schema tag must be in range [0, $maxSchemaTag]: $tag" }

  val memberTypes = schemaAnnotation.members

  val duplicatedMembers = memberTypes.groupBy { it }.filterValues { it.size > 1 }.keys
  if (duplicatedMembers.isNotEmpty()) {
    throw IllegalArgumentException(
      buildString {
        append("Schema contains repeated member")
        if (duplicatedMembers.size > 1) {
          append('s')
        }
        duplicatedMembers.joinTo(this, prefix = "\n\n- ", separator = "\n- ") { it.qualifiedName!! }
      },
    )
  }

  val widgets = mutableListOf<Widget>()
  val layoutModifiers = mutableListOf<LayoutModifier>()
  for (memberType in memberTypes) {
    val widgetAnnotation = memberType.findAnnotation<WidgetAnnotation>()
    val layoutModifierAnnotation = memberType.findAnnotation<LayoutModifierAnnotation>()

    if ((widgetAnnotation == null) == (layoutModifierAnnotation == null)) {
      throw IllegalArgumentException(
        "${memberType.qualifiedName} must be annotated with either @Widget or @LayoutModifier",
      )
    } else if (widgetAnnotation != null) {
      widgets += parseWidget(tag, memberType, widgetAnnotation)
    } else if (layoutModifierAnnotation != null) {
      layoutModifiers += parseLayoutModifier(tag, memberType, layoutModifierAnnotation)
    } else {
      throw AssertionError()
    }
  }

  val badWidgets = widgets.groupBy(Widget::tag).filterValues { it.size > 1 }
  if (badWidgets.isNotEmpty()) {
    throw IllegalArgumentException(
      buildString {
        appendLine("Schema @Widget tags must be unique")
        for ((widgetTag, group) in badWidgets) {
          append("\n- @Widget($widgetTag): ")
          group.joinTo(this) { it.type.qualifiedName!! }
        }
      },
    )
  }

  val badLayoutModifiers = layoutModifiers.groupBy(LayoutModifier::tag).filterValues { it.size > 1 }
  if (badLayoutModifiers.isNotEmpty()) {
    throw IllegalArgumentException(
      buildString {
        appendLine("Schema @LayoutModifier tags must be unique")
        for ((modifierTag, group) in badLayoutModifiers) {
          append("\n- @LayoutModifier($modifierTag): ")
          group.joinTo(this) { it.type.qualifiedName!! }
        }
      },
    )
  }

  val widgetScopes = widgets
    .flatMap { it.traits }
    .filterIsInstance<Widget.Children>()
    .mapNotNull { it.scope }
  val layoutModifierScopes = layoutModifiers
    .flatMap { it.scopes }
  val scopes = buildSet {
    addAll(widgetScopes)
    addAll(layoutModifierScopes)
  }

  val badDependencyTags = schemaAnnotation.dependencies
    .groupBy { it.tag }
    .filterValues { it.size > 1 }
  if (badDependencyTags.isNotEmpty()) {
    throw IllegalArgumentException(
      buildString {
        appendLine("Schema dependency tags must be unique")
        for ((dependencyTag, group) in badDependencyTags) {
          append("\n- Dependency tag $dependencyTag: ")
          group.joinTo(this) { it.schema.qualifiedName!! }
        }
      },
    )
  }

  val badDependencyTypes = schemaAnnotation.dependencies
    .groupBy { it.schema }
    .filterValues { it.size > 1 }
    .keys
  if (badDependencyTypes.isNotEmpty()) {
    throw IllegalArgumentException(
      buildString {
        append("Schema contains repeated ")
        append(if (badDependencyTypes.size > 1) "dependencies" else "dependency")
        badDependencyTypes.joinTo(this, prefix = "\n\n- ", separator = "\n- ") { it.qualifiedName!! }
      },
    )
  }

  val dependencies = schemaAnnotation.dependencies
    .map {
      require(it.tag in 1..maxSchemaTag.toInt()) {
        "Dependency ${it.schema.qualifiedName} tag must be in range (0, $maxSchemaTag]: ${it.tag}"
      }
      val schema = parseSchema(it.schema, it.tag.toUIntExact())
      require(schema.dependencies.isEmpty()) {
        "Schema dependency ${it.schema.qualifiedName} also has its own dependencies. " +
          "For now, only a single level of dependencies is supported."
      }
      schema
    }

  val schema = Schema(
    schemaType.simpleName!!,
    schemaType.java.packageName,
    scopes.toList(),
    widgets,
    layoutModifiers,
    dependencies,
  )

  val duplicatedWidgets = (listOf(schema) + dependencies)
    .flatMap { it.widgets.map { widget -> widget to it } }
    .groupBy { it.first.type }
    .filterValues { it.size > 1 }
    .mapValues { it.value.map(Pair<*, Schema>::second) }
  if (duplicatedWidgets.isNotEmpty()) {
    throw IllegalArgumentException(
      buildString {
        appendLine("Schema dependency tree contains duplicated widgets")
        for ((widget, schemas) in duplicatedWidgets) {
          append("\n- ${widget.qualifiedName}: ")
          schemas.joinTo(this) { "${it.`package`}.${it.name}" }
        }
      },
    )
  }

  return schema
}

private fun parseWidget(
  schemaTag: UInt,
  memberType: KClass<*>,
  annotation: WidgetAnnotation,
): Widget {
  require(annotation.tag in 1 until maxMemberTag.toInt()) {
    "@Widget ${memberType.qualifiedName} tag must be in range [1, $maxMemberTag): ${annotation.tag}"
  }
  val tag = schemaTag * maxMemberTag + annotation.tag.toUIntExact()

  val traits = if (memberType.isData) {
    memberType.primaryConstructor!!.parameters.map {
      val property = it.findAnnotation<PropertyAnnotation>()
      val children = it.findAnnotation<ChildrenAnnotation>()
      val defaultExpression = it.findAnnotation<DefaultAnnotation>()?.expression

      if (property != null) {
        if (it.type.isSubtypeOf(eventType) || it.type.isSubtypeOf(optionalEventType)) {
          val arguments = it.type.arguments.dropLast(1) // Drop return type.
          require(arguments.size <= 1) {
            "@Property ${memberType.qualifiedName}#${it.name} lambda type can only have zero or one arguments. Found: $arguments"
          }
          Widget.Event(property.tag.toUIntExact(), it.name!!, defaultExpression, arguments.singleOrNull()?.type)
        } else {
          Widget.Property(property.tag.toUIntExact(), it.name!!, it.type, defaultExpression)
        }
      } else if (children != null) {
        require(it.type.isSubtypeOf(childrenType)) {
          "@Children ${memberType.qualifiedName}#${it.name} must be of type '() -> Unit'"
        }
        var scope: KClass<*>? = null
        var arguments = it.type.arguments.dropLast(1) // Drop return type.
        if (it.type.annotations.any(ExtensionFunctionType::class::isInstance)) {
          val receiverType = it.type.arguments.first().type
          val receiverClassifier = receiverType?.classifier
          require(receiverClassifier is KClass<*> && receiverType.arguments.isEmpty()) {
            "@Children ${memberType.qualifiedName}#${it.name} lambda receiver can only be a class. Found: $receiverType"
          }
          scope = receiverClassifier
          arguments = arguments.drop(1)
        }
        require(arguments.isEmpty()) {
          "@Children ${memberType.qualifiedName}#${it.name} lambda type must not have any arguments. Found: $arguments"
        }
        Widget.Children(children.tag.toUIntExact(), it.name!!, defaultExpression, scope)
      } else {
        throw IllegalArgumentException("Unannotated parameter \"${it.name}\" on ${memberType.qualifiedName}")
      }
    }
  } else if (memberType.objectInstance != null) {
    emptyList()
  } else {
    throw IllegalArgumentException(
      "@Widget ${memberType.qualifiedName} must be 'data' class or 'object'",
    )
  }

  val badChildren = traits.filterIsInstance<Widget.Children>()
    .groupBy(Widget.Children::tag)
    .filterValues { it.size > 1 }
  if (badChildren.isNotEmpty()) {
    throw IllegalArgumentException(
      buildString {
        appendLine("${memberType.qualifiedName}'s @Children tags must be unique")
        for ((childTag, group) in badChildren) {
          append("\n- @Children($childTag): ")
          group.joinTo(this) { it.name }
        }
      },
    )
  }

  val badProperties = traits.filterIsInstance<Widget.Property>()
    .groupBy(Widget.Property::tag)
    .filterValues { it.size > 1 }
  if (badProperties.isNotEmpty()) {
    throw IllegalArgumentException(
      buildString {
        appendLine("${memberType.qualifiedName}'s @Property tags must be unique")
        for ((propertyTag, group) in badProperties) {
          append("\n- @Property($propertyTag): ")
          group.joinTo(this) { it.name }
        }
      },
    )
  }

  return Widget(tag, memberType, traits)
}

private fun parseLayoutModifier(
  schemaTag: UInt,
  memberType: KClass<*>,
  annotation: LayoutModifierAnnotation,
): LayoutModifier {
  require(annotation.tag in 1 until maxMemberTag.toInt()) {
    "@LayoutModifier ${memberType.qualifiedName} tag must be in range [1, $maxMemberTag): ${annotation.tag}"
  }
  require(annotation.scopes.isNotEmpty()) {
    "@LayoutModifier ${memberType.qualifiedName} must have at least one scope."
  }
  val tag = schemaTag * maxMemberTag + annotation.tag.toUIntExact()

  val properties = if (memberType.isData) {
    memberType.primaryConstructor!!.parameters.map {
      val defaultExpression = it.findAnnotation<DefaultAnnotation>()?.expression
      LayoutModifier.Property(it.name!!, it.type, defaultExpression)
    }
  } else if (memberType.objectInstance != null) {
    emptyList()
  } else {
    throw IllegalArgumentException(
      "@LayoutModifier ${memberType.qualifiedName} must be 'data' class or 'object'",
    )
  }

  return LayoutModifier(
    tag,
    annotation.scopes.toList(),
    memberType,
    properties,
  )
}
