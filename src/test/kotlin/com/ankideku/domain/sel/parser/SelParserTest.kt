package com.ankideku.domain.sel.parser

import com.ankideku.domain.sel.ast.SelParseException
import com.ankideku.domain.sel.ast.SelArray
import com.ankideku.domain.sel.ast.SelBoolean
import com.ankideku.domain.sel.ast.SelNull
import com.ankideku.domain.sel.ast.SelNumber
import com.ankideku.domain.sel.ast.SelOperation
import com.ankideku.domain.sel.ast.SelParser
import com.ankideku.domain.sel.ast.SelQuery
import com.ankideku.domain.sel.ast.SelOrderDirection
import com.ankideku.domain.sel.ast.SelString
import com.ankideku.domain.sel.model.EntityType
import kotlin.test.*

class SelParserTest {

    // ==================== Primitive Parsing ====================

    @Test
    fun `parse string primitive`() {
        val node = SelParser.parse("\"hello\"")
        assertIs<SelString>(node)
        assertEquals("hello", node.value)
    }

    @Test
    fun `parse integer number`() {
        val node = SelParser.parse("42")
        assertIs<SelNumber>(node)
        assertEquals(42L, node.value)
    }

    @Test
    fun `parse floating point number`() {
        val node = SelParser.parse("3.14")
        assertIs<SelNumber>(node)
        assertEquals(3.14, node.toDouble(), 0.001)
    }

    @Test
    fun `parse negative number`() {
        val node = SelParser.parse("-10")
        assertIs<SelNumber>(node)
        assertEquals(-10L, node.value)
    }

    @Test
    fun `parse boolean true`() {
        val node = SelParser.parse("true")
        assertIs<SelBoolean>(node)
        assertTrue(node.value)
        assertSame(SelBoolean.TRUE, node)
    }

    @Test
    fun `parse boolean false`() {
        val node = SelParser.parse("false")
        assertIs<SelBoolean>(node)
        assertFalse(node.value)
        assertSame(SelBoolean.FALSE, node)
    }

    @Test
    fun `parse null`() {
        val node = SelParser.parse("null")
        assertIs<SelNull>(node)
        assertNull(node.value)
    }

    // ==================== Array Parsing ====================

    @Test
    fun `parse empty array`() {
        val node = SelParser.parse("[]")
        assertIs<SelArray>(node)
        assertTrue(node.isEmpty())
    }

    @Test
    fun `parse array with primitives`() {
        val node = SelParser.parse("""[1, "two", true, null]""")
        assertIs<SelArray>(node)
        assertEquals(4, node.size)
        assertIs<SelNumber>(node[0])
        assertIs<SelString>(node[1])
        assertIs<SelBoolean>(node[2])
        assertIs<SelNull>(node[3])
    }

    @Test
    fun `parse nested arrays`() {
        val node = SelParser.parse("""[[1, 2], [3, 4]]""")
        assertIs<SelArray>(node)
        assertEquals(2, node.size)
        assertIs<SelArray>(node[0])
        assertIs<SelArray>(node[1])
    }

    // ==================== Operation Parsing ====================

    @Test
    fun `parse simple operation with single argument`() {
        val node = SelParser.parse("""{ "field": "example" }""")
        assertIs<SelOperation>(node)
        assertEquals("field", node.operator)
        assertEquals(1, node.arguments.size)
        val arg = node.arguments[0]
        assertIs<SelString>(arg)
        assertEquals("example", arg.value)
    }

    @Test
    fun `parse operation with array arguments`() {
        val node = SelParser.parse("""{ "==": [1, 2] }""")
        assertIs<SelOperation>(node)
        assertEquals("==", node.operator)
        assertEquals(2, node.arguments.size)
        assertIs<SelNumber>(node.arguments[0])
        assertIs<SelNumber>(node.arguments[1])
    }

    @Test
    fun `parse nested operations`() {
        val node = SelParser.parse("""{ "and": [{ "==": [1, 1] }, { "!=": [2, 3] }] }""")
        assertIs<SelOperation>(node)
        assertEquals("and", node.operator)
        assertEquals(2, node.arguments.size)

        val firstCondition = node.arguments[0]
        assertIs<SelOperation>(firstCondition)
        assertEquals("==", firstCondition.operator)

        val secondCondition = node.arguments[1]
        assertIs<SelOperation>(secondCondition)
        assertEquals("!=", secondCondition.operator)
    }

    @Test
    fun `parse field access with nested path`() {
        // When field has an array value, the array elements become the arguments directly
        // So { "field": ["example", "changes"] } means field("example", "changes")
        val node = SelParser.parse("""{ "field": ["example", "changes"] }""")
        assertIs<SelOperation>(node)
        assertEquals("field", node.operator)
        assertEquals(2, node.arguments.size)

        val arg0 = node.arguments[0]
        assertIs<SelString>(arg0)
        assertEquals("example", arg0.value)

        val arg1 = node.arguments[1]
        assertIs<SelString>(arg1)
        assertEquals("changes", arg1.value)
    }

    @Test
    fun `parse isEmpty operation`() {
        val node = SelParser.parse("""{ "isEmpty": { "field": "example" } }""")
        assertIs<SelOperation>(node)
        assertEquals("isEmpty", node.operator)
        assertEquals(1, node.arguments.size)

        val innerOp = node.arguments[0]
        assertIs<SelOperation>(innerOp)
        assertEquals("field", innerOp.operator)
    }

    @Test
    fun `parse complex query with contains and len`() {
        val json = """
        {
            "and": [
                { "contains": [
                    { "field": ["example", "changes"] },
                    { "field": ["kanji", "original"] }
                ]},
                { ">=": [
                    { "len": { "field": ["example", "changes"] } },
                    { "+": [
                        { "len": { "field": ["kanji", "original"] } },
                        4
                    ]}
                ]}
            ]
        }
        """.trimIndent()

        val node = SelParser.parse(json)
        assertIs<SelOperation>(node)
        assertEquals("and", node.operator)
        assertEquals(2, node.arguments.size)
    }

    // ==================== Error Cases ====================

    @Test
    fun `fail on invalid JSON`() {
        val exception = assertFailsWith<SelParseException> {
            SelParser.parse("{ invalid json }")
        }
        assertEquals("$", exception.jsonPath)
    }

    @Test
    fun `fail on object with multiple keys`() {
        val exception = assertFailsWith<SelParseException> {
            SelParser.parse("""{ "a": 1, "b": 2 }""")
        }
        assertTrue(exception.message!!.contains("exactly one key"))
    }

    @Test
    fun `fail on empty object`() {
        val exception = assertFailsWith<SelParseException> {
            SelParser.parse("{}")
        }
        assertTrue(exception.message!!.contains("exactly one key"))
    }

    // ==================== Query Parsing ====================

    @Test
    fun `parse minimal query`() {
        val query = SelParser.parseQuery("""
        {
            "target": "Note",
            "where": { "==": [1, 1] }
        }
        """.trimIndent())

        assertEquals(EntityType.Note, query.target)
        assertIs<SelOperation>(query.where)
        assertNull(query.alias)
        assertNull(query.result)
        assertNull(query.orderBy)
        assertNull(query.limit)
    }

    @Test
    fun `parse query with alias`() {
        val query = SelParser.parseQuery("""
        {
            "target": "Note",
            "alias": "n",
            "where": { "==": [1, 1] }
        }
        """.trimIndent())

        assertEquals(EntityType.Note, query.target)
        assertEquals("n", query.alias)
    }

    @Test
    fun `parse query with result`() {
        val query = SelParser.parseQuery("""
        {
            "target": "Suggestion",
            "where": { "==": [1, 1] },
            "result": { "prop": "status" }
        }
        """.trimIndent())

        assertEquals(EntityType.Suggestion, query.target)
        assertNotNull(query.result)
        assertIs<SelOperation>(query.result)
        assertEquals("prop", (query.result as SelOperation).operator)
    }

    @Test
    fun `parse full query with all fields`() {
        val query = SelParser.parseQuery("""
        {
            "target": "Suggestion",
            "alias": "s",
            "where": { "isEmpty": { "field": "example" } },
            "result": { "prop": "status" },
            "orderBy": [
                { "field": "createdAt", "direction": "Desc" },
                { "field": "id" }
            ],
            "limit": 50
        }
        """.trimIndent())

        assertEquals(EntityType.Suggestion, query.target)
        assertEquals("s", query.alias)
        assertIs<SelOperation>(query.where)
        assertNotNull(query.result)
        assertEquals(2, query.orderBy!!.size)
        assertEquals("createdAt", query.orderBy!![0].field)
        assertEquals(SelOrderDirection.Desc, query.orderBy!![0].direction)
        assertEquals("id", query.orderBy!![1].field)
        assertEquals(SelOrderDirection.Asc, query.orderBy!![1].direction) // default
        assertEquals(50, query.limit)
    }

    @Test
    fun `parse query with HistoryEntry target`() {
        val query = SelParser.parseQuery("""
        {
            "target": "HistoryEntry",
            "where": { "==": [{ "prop": "action" }, "accepted"] }
        }
        """.trimIndent())

        assertEquals(EntityType.HistoryEntry, query.target)
    }

    @Test
    fun `parse query with Session target`() {
        val query = SelParser.parseQuery("""
        {
            "target": "Session",
            "where": { "==": [{ "prop": "status" }, "completed"] }
        }
        """.trimIndent())

        assertEquals(EntityType.Session, query.target)
    }

    @Test
    fun `parse orderBy with desc boolean shorthand`() {
        val query = SelParser.parseQuery("""
        {
            "target": "Note",
            "where": { "==": [1, 1] },
            "orderBy": [{ "prop": "createdAt", "desc": true }]
        }
        """.trimIndent())

        assertEquals(SelOrderDirection.Desc, query.orderBy!![0].direction)
    }

    @Test
    fun `fail on missing target`() {
        val exception = assertFailsWith<SelParseException> {
            SelParser.parseQuery("""{ "where": { "==": [1, 1] } }""")
        }
        assertTrue(exception.message!!.contains("target"))
    }

    @Test
    fun `fail on missing where`() {
        val exception = assertFailsWith<SelParseException> {
            SelParser.parseQuery("""{ "target": "Note" }""")
        }
        assertTrue(exception.message!!.contains("where"))
    }

    @Test
    fun `fail on invalid target`() {
        val exception = assertFailsWith<SelParseException> {
            SelParser.parseQuery("""{ "target": "InvalidType", "where": {} }""")
        }
        assertTrue(exception.message!!.contains("Invalid target"))
    }

    @Test
    fun `fail on invalid order direction`() {
        val exception = assertFailsWith<SelParseException> {
            SelParser.parseQuery("""
            {
                "target": "Note",
                "where": { "==": [1, 1] },
                "orderBy": [{ "field": "id", "direction": "Invalid" }]
            }
            """.trimIndent())
        }
        assertTrue(exception.message!!.contains("Invalid direction"))
    }

    // ==================== Nested Query Parsing ====================

    @Test
    fun `parse query with nested subquery`() {
        val query = SelParser.parseQuery("""
        {
            "target": "Note",
            "alias": "n",
            "where": { "exists": { "query": {
                "target": "Suggestion",
                "where": { "==": [{ "prop": "noteId" }, { "ref": ["n", "id"] }] }
            }}}
        }
        """.trimIndent())

        assertEquals(EntityType.Note, query.target)
        assertEquals("n", query.alias)

        val where = query.where
        assertIs<SelOperation>(where)
        assertEquals("exists", where.operator)

        val queryOp = where.arguments[0]
        assertIs<SelOperation>(queryOp)
        assertEquals("query", queryOp.operator)

        val innerQuery = queryOp.arguments[0]
        assertIs<SelQuery>(innerQuery)
        assertEquals(EntityType.Suggestion, innerQuery.target)
    }

    @Test
    fun `parseQuery parses query object directly`() {
        // parseQuery handles query objects directly (without { "query": {...} } wrapper)
        val query = SelParser.parseQuery("""
        {
            "target": "Suggestion",
            "where": { "==": [{ "prop": "status" }, "pending"] }
        }
        """.trimIndent())

        assertEquals(EntityType.Suggestion, query.target)
        assertIs<SelOperation>(query.where)
    }

    // ==================== Real-World Query Examples ====================

    @Test
    fun `parse notes with empty example field query`() {
        val query = SelParser.parseQuery("""
        {
            "target": "Note",
            "where": { "isEmpty": { "field": "example" } }
        }
        """.trimIndent())

        assertEquals(EntityType.Note, query.target)
        val where = query.where as SelOperation
        assertEquals("isEmpty", where.operator)
    }

    @Test
    fun `parse pending suggestions query`() {
        val query = SelParser.parseQuery("""
        {
            "target": "Suggestion",
            "where": { "==": [{ "prop": "status" }, "pending"] }
        }
        """.trimIndent())

        assertEquals(EntityType.Suggestion, query.target)
        val where = query.where as SelOperation
        assertEquals("==", where.operator)
        assertEquals(2, where.arguments.size)

        val propOp = where.arguments[0] as SelOperation
        assertEquals("prop", propOp.operator)
    }

    @Test
    fun `parse notes where reading starts with kana query`() {
        val query = SelParser.parseQuery("""
        {
            "target": "Note",
            "where": { "startsWith": [
                { "field": "reading" },
                { "field": "kana" }
            ]}
        }
        """.trimIndent())

        assertEquals(EntityType.Note, query.target)
        val where = query.where as SelOperation
        assertEquals("startsWith", where.operator)
        assertEquals(2, where.arguments.size)
    }

    @Test
    fun `parse notes with accepted suggestions (exists subquery)`() {
        val query = SelParser.parseQuery("""
        {
            "target": "Note",
            "alias": "n",
            "where": { "exists": { "query": {
                "target": "Suggestion",
                "where": { "and": [
                    { "==": [{ "prop": "noteId" }, { "ref": ["n", "id"] }] },
                    { "==": [{ "prop": "status" }, "accepted"] }
                ]}
            }}}
        }
        """.trimIndent())

        assertEquals(EntityType.Note, query.target)
        assertEquals("n", query.alias)

        val where = query.where
        assertIs<SelOperation>(where)
        assertEquals("exists", where.operator)
    }

    @Test
    fun `parse scalar subquery returning status`() {
        val query = SelParser.parseQuery("""
        {
            "target": "Note",
            "alias": "n",
            "where": { "==": [
                { "query": {
                    "target": "Suggestion",
                    "where": { "==": [{ "prop": "noteId" }, { "ref": ["n", "id"] }] },
                    "result": { "prop": "status" },
                    "orderBy": [{ "prop": "createdAt", "desc": true }],
                    "limit": 1
                }},
                "accepted"
            ]}
        }
        """.trimIndent())

        assertEquals(EntityType.Note, query.target)
        assertEquals("n", query.alias)

        val where = query.where
        assertIs<SelOperation>(where)
        assertEquals("==", where.operator)
        assertEquals(2, where.arguments.size)

        val queryOp = where.arguments[0]
        assertIs<SelOperation>(queryOp)
        assertEquals("query", queryOp.operator)

        val innerQuery = queryOp.arguments[0]
        assertIs<SelQuery>(innerQuery)
        assertNotNull(innerQuery.result)
        assertEquals(1, innerQuery.limit)
    }
}
