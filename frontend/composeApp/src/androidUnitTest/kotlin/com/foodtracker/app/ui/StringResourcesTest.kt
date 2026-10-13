package com.foodtracker.app.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.w3c.dom.Element

/** Spanish (base) and English must define the same strings with the same placeholders. */
class StringResourcesTest {

    private val placeholder = Regex("""%(\d+)\$[ds]""")

    /** key -> sorted placeholders, for `<string>` and each `<plurals>` item (key#quantity). */
    private fun load(qualifier: String): Map<String, List<String>> {
        val file = File("src/commonMain/composeResources/$qualifier/strings.xml")
        assertTrue(file.exists(), "missing ${file.path}")
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val entries = mutableMapOf<String, List<String>>()
        val strings = document.getElementsByTagName("string")
        for (i in 0 until strings.length) {
            val node = strings.item(i) as Element
            entries[node.getAttribute("name")] = placeholders(node.textContent)
        }
        val plurals = document.getElementsByTagName("plurals")
        for (i in 0 until plurals.length) {
            val node = plurals.item(i) as Element
            val items = node.getElementsByTagName("item")
            for (j in 0 until items.length) {
                val item = items.item(j) as Element
                entries["${node.getAttribute("name")}#${item.getAttribute("quantity")}"] = placeholders(item.textContent)
            }
        }
        return entries
    }

    /** `<string-array>` name -> its item count. */
    private fun loadArrays(qualifier: String): Map<String, Int> {
        val file = File("src/commonMain/composeResources/$qualifier/strings.xml")
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val arrays = document.getElementsByTagName("string-array")
        return (0 until arrays.length).associate { i ->
            val node = arrays.item(i) as Element
            node.getAttribute("name") to node.getElementsByTagName("item").length
        }
    }

    private fun placeholders(text: String) = placeholder.findAll(text).map { it.value }.distinct().sorted().toList()

    @Test
    fun `both languages define the same keys`() {
        val spanish = load("values")
        val english = load("values-en")
        assertEquals(spanish.keys.sorted(), english.keys.sorted())
    }

    @Test
    fun `every key uses the same placeholders in both languages`() {
        val spanish = load("values")
        val english = load("values-en")
        for ((key, args) in spanish) {
            assertEquals(args, english[key], "placeholders differ for $key")
        }
    }

    @Test
    fun `no string relies on Android-style backslash escapes`() {
        for (qualifier in listOf("values", "values-en")) {
            val text = File("src/commonMain/composeResources/$qualifier/strings.xml").readText()
            assertTrue('\\' !in text, "$qualifier/strings.xml contains a backslash; Compose resources show it literally")
        }
    }

    @Test
    fun `both languages define the same string arrays with the same sizes`() {
        val spanish = loadArrays("values")
        assertEquals(spanish, loadArrays("values-en"))
        // Home indexes these by ISO day number and month number.
        assertEquals(7, spanish["weekday_names"])
        assertEquals(12, spanish["month_names"])
    }
}
