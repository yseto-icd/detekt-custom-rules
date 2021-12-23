package com.sodatsu.detekt

import com.google.common.truth.Truth.assertThat
import io.github.detekt.test.utils.KotlinCoreEnvironmentWrapper
import io.github.detekt.test.utils.createEnvironment
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

internal class MyBatisSqlInjectableSpec {

    val dollar = '$'
    val quote = '"'

    @Test
    fun `report mybatis with dollar substitution insert`() {
        val code = """
        @Insert(
            ${quote}${quote}${quote}
                INSERT INTO hoge
                    (hoge_id,
                    updated_at
                    )
                VALUES
                    (#{hogeId},
                    ${dollar}{updated_at}
                )
            ${quote}${quote}${quote})
        interface A 
        """
        val findings = MyBatisSqlInjectableRule(Config.empty).compileAndLintWithContext(env, code)
        println(findings)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `report mybatis with dollar substitution select`() {
        val code = """
        @Select(
            ${quote}${quote}${quote}
                SELECT
                    id,
                    hoge_id
                FROM
                    hoge
                WHERE
                    hoge = ${dollar}{hoge}
                )
            ${quote}${quote}${quote})
        interface A 
        """
        val findings = MyBatisSqlInjectableRule(Config.empty).compileAndLintWithContext(env, code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `report mybatis with hash prepared statements insert`() {
        val code = """
        @Insert(
            ${quote}${quote}${quote}
                INSERT INTO hoge
                    (hoge_id,
                    updated_at
                    )
                VALUES
                    (#{hogeId},
                    #{updated_at}
                )
            ${quote}${quote}${quote})
        interface A 
        """
        val findings = MyBatisSqlInjectableRule(Config.empty).compileAndLintWithContext(env, code)
        assertThat(findings).hasSize(0)
    }

    @Test
    fun `report mybatis with hash prepared statements select`() {
        val code = """
        @Select(
            ${quote}${quote}${quote}
                SELECT
                    id,
                    hoge_id,
                FROM
                    hoge
                WHERE
                    hoge = #{hoge}
            ${quote}${quote}${quote}
        interface A 
        """
        val findings = MyBatisSqlInjectableRule(Config.empty).compileAndLintWithContext(env, code)
        assertThat(findings).hasSize(0)
    }

    @Test
    fun `doesn't report not mybatis`() {
        val code = """
        @Test("${dollar}{}")
        interface A
        """
        val findings = MyBatisSqlInjectableRule(Config.empty).compileAndLintWithContext(env, code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `doesn't report dollar not in annotation`() {
        val code = """
            val test = "Insert${dollar}{}"
            val test = "Select ${dollar}{}"
        """
        val findings = MyBatisSqlInjectableRule(Config.empty).compileAndLintWithContext(env, code)
        assertThat(findings).isEmpty()
    }

    private val env: KotlinCoreEnvironment
        get() = envWrapper.env

    companion object {
        private lateinit var envWrapper: KotlinCoreEnvironmentWrapper

        @BeforeClass
        @JvmStatic
        fun setUp() {
            envWrapper = createEnvironment()
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            envWrapper.dispose()
        }
    }
}
