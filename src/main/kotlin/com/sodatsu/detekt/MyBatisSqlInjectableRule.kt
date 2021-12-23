package com.sodatsu.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.util.isAnnotated

class MyBatisSqlInjectableRule(config: Config) : Rule(config) {
    override val issue = Issue(
        javaClass.simpleName,
        Severity.CodeSmell,
        "SQL Injectable with String Substitution in MyBatis",
        Debt.FIVE_MINS,
    )
    private val mybatisAnnotations = setOf("Insert", "Select")

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        if (klass.isAnnotated && sqlInjectableMybatisAnnotation(klass)) {
            report(CodeSmell(issue, Entity.atName(klass),
                """
                    SQL injectable with dollar substitution in MyBatis.
                    Avoid to use dollar substitution or
                    must sanitize parameters.
                    """))
        }
    }
    private val quote = "\""
    private fun sqlInjectableMybatisAnnotation(klass: KtClass): Boolean {
        return klass.annotationEntries
            .find { it.typeReference?.text in mybatisAnnotations }
            ?.run {
                valueArguments
                    .map { it.getArgumentExpression()?.text }
                    .map { it?.replace(quote, "") }
                    .find { it?.contains("\${") ?: false }
            } != null
    }

}
