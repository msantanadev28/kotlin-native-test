package lienzo.compiler

import java.io.File

enum class TokenType {
    TAG_OPEN,           // <
    TAG_CLOSE,          // >
    TAG_SELF_CLOSE,     // />
    TAG_END_OPEN,       // </
    IDENTIFIER,         // Window, Button, title, width, height, text, onClick
    EQUALS,             // =
    STRING,             // "hello"
    EOF
}

data class Token(
    val type: TokenType,
    val value: String,
    val line: Int,
    val col: Int
)

class Lexer(private val source: String) {
    private var pos = 0
    private var line = 1
    private var col = 1
    private val tokens = mutableListOf<Token>()

    private fun peek(): Char = if (pos < source.length) source[pos] else '\u0000'
    private fun peek(n: Int): String {
        val end = minOf(pos + n, source.length)
        return source.substring(pos, end)
    }

    private fun advance(): Char {
        val c = peek()
        pos++
        if (c == '\n') {
            line++
            col = 1
        } else {
            col++
        }
        return c
    }

    private fun skipWhitespace() {
        while (pos < source.length) {
            val c = peek()
            if (c.isWhitespace()) {
                advance()
            } else {
                break
            }
        }
    }

    private fun emit(type: TokenType, value: String) {
        tokens.add(Token(type, value, line, col))
    }

    private fun isIdentStart(): Boolean {
        val c = peek()
        return c.isLetter() || c == '_'
    }

    fun tokenize(): List<Token> {
        while (pos < source.length) {
            skipWhitespace()
            if (pos >= source.length) break
            when {
                peek(4) == "<!--" -> readComment()
                peek() == '<' && peek(2) == "</" -> readTagEndOpen()
                peek() == '<' -> readTagOpen()
                peek() == '/' && peek(2) == "/>" -> readSelfClose()
                peek() == '>' -> {
                    emit(TokenType.TAG_CLOSE, ">")
                    advance()
                }
                peek() == '"' -> readString()
                peek() == '=' -> {
                    emit(TokenType.EQUALS, "=")
                    advance()
                }
                isIdentStart() -> readIdentifier()
                else -> advance() // skip unknown
            }
        }
        emit(TokenType.EOF, "")
        return tokens
    }

    private fun readComment() {
        repeat(4) { advance() } // consume <!--
        while (pos < source.length && peek(3) != "-->") {
            advance()
        }
        repeat(3) { advance() } // consume -->
    }

    private fun readTagEndOpen() {
        repeat(2) { advance() } // consume </
        emit(TokenType.TAG_END_OPEN, "</")
    }

    private fun readTagOpen() {
        advance() // consume <
        emit(TokenType.TAG_OPEN, "<")
    }

    private fun readSelfClose() {
        repeat(2) { advance() } // consume />
        emit(TokenType.TAG_SELF_CLOSE, "/>")
    }

    private fun readString() {
        advance() // opening "
        val sb = StringBuilder()
        while (pos < source.length) {
            val c = peek()
            if (c == '\\') {
                advance() // consume \
                if (pos < source.length) {
                    sb.append(advance()) // consume escaped character
                }
            } else if (c == '"') {
                break
            } else {
                sb.append(advance())
            }
        }
        advance() // closing "
        emit(TokenType.STRING, sb.toString())
    }

    private fun readIdentifier() {
        val sb = StringBuilder()
        while (pos < source.length && (peek().isLetterOrDigit() || peek() == '-' || peek() == '_')) {
            sb.append(advance())
        }
        emit(TokenType.IDENTIFIER, sb.toString())
    }
}

sealed class Node
data class DocumentNode(val children: List<ElementNode>) : Node()
data class ElementNode(
    val tag: String,
    val attributes: Map<String, String>,
    val children: List<ElementNode>
) : Node()

class Parser(private val tokens: List<Token>) {
    private var pos = 0

    private fun current(): Token = if (pos < tokens.size) tokens[pos] else Token(TokenType.EOF, "", 0, 0)
    private fun advance(): Token {
        val t = current()
        if (t.type != TokenType.EOF) pos++
        return t
    }

    private fun expect(type: TokenType): Token {
        val t = current()
        if (t.type != type) {
            error("Expected token $type but got ${t.type} '${t.value}' at line ${t.line}, col ${t.col}")
        }
        return advance()
    }

    private fun isEof() = current().type == TokenType.EOF

    fun parse(): DocumentNode {
        val children = mutableListOf<ElementNode>()
        while (!isEof()) {
            children.add(parseElement())
        }
        return DocumentNode(children)
    }

    private fun parseElement(): ElementNode {
        expect(TokenType.TAG_OPEN)
        val tag = expect(TokenType.IDENTIFIER).value
        val attributes = parseAttributes()

        return if (current().type == TokenType.TAG_SELF_CLOSE) {
            advance()
            ElementNode(tag, attributes, emptyList())
        } else {
            expect(TokenType.TAG_CLOSE)
            val children = parseChildren(tag)
            ElementNode(tag, attributes, children)
        }
    }

    private fun parseAttributes(): Map<String, String> {
        val attrs = mutableMapOf<String, String>()
        while (current().type == TokenType.IDENTIFIER) {
            val key = advance().value
            expect(TokenType.EQUALS)
            val value = expect(TokenType.STRING).value
            attrs[key] = value
        }
        return attrs
    }

    private fun parseChildren(parentTag: String): List<ElementNode> {
        val children = mutableListOf<ElementNode>()
        while (current().type != TokenType.TAG_END_OPEN) {
            if (isEof()) error("Unclosed tag <$parentTag>")
            children.add(parseElement())
        }
        expect(TokenType.TAG_END_OPEN)
        val closingTag = expect(TokenType.IDENTIFIER).value
        if (closingTag != parentTag) {
            error("Mismatched closing tag: expected </$parentTag> but got </$closingTag>")
        }
        expect(TokenType.TAG_CLOSE)
        return children
    }
}

class KotlinCodeGen(
    private val packageName: String,
    private val functionName: String
) {
    fun generate(doc: DocumentNode): String = buildString {
        appendLine("package $packageName")
        appendLine()
        appendLine("import lienzo.runtime.*")
        appendLine("import lienzo.runtime.widgets.*")
        appendLine("import ui.*")
        appendLine()
        appendLine("fun $functionName(): Widget {")
        appendLine("    return ${generateElement(doc.children.first(), indent = 1)}")
        appendLine("}")
    }

    private fun generateElement(node: ElementNode, indent: Int): String {
        val pad = "    ".repeat(indent)
        val fn = node.tag.replaceFirstChar { it.lowercaseChar() }
        val args = node.attributes.entries.joinToString(", ") { (k, v) ->
            if (k == "onClick") {
                "$k = ::$v"
            } else if (v.startsWith("{") && v.endsWith("}")) {
                "$k = ${v.substring(1, v.length - 1)}"
            } else {
                val isNum = v.toIntOrNull() != null || v.toFloatOrNull() != null
                val isBool = v == "true" || v == "false"
                if (isNum || isBool) {
                    "$k = $v"
                } else {
                    "$k = \"$v\""
                }
            }
        }
        return if (node.children.isEmpty()) {
            "$fn($args)"
        } else {
            val childLines = node.children.joinToString("\n") {
                "$pad    ${generateElement(it, indent + 1)}"
            }
            "$fn($args) {\n$childLines\n$pad}"
        }
    }
}

fun main(args: Array<String>) {
    val inputDir = File(args[0])
    val outputDir = File(args[1])
    val packageName = args.getOrElse(2) { "ui.generated" }

    if (!inputDir.exists()) return
    outputDir.mkdirs()

    inputDir.walkTopDown()
        .filter { it.extension == "lienzo" }
        .forEach { file ->
            val source = file.readText()
            val tokens = Lexer(source).tokenize()
            val ast = Parser(tokens).parse()
            val code = KotlinCodeGen(packageName, file.nameWithoutExtension).generate(ast)
            val outFile = File(outputDir, "${file.nameWithoutExtension}.kt")
            outFile.writeText(code)
            println("Compiled ${file.absolutePath} -> ${outFile.absolutePath}")
        }
}
