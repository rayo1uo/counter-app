package com.example.counter.data
import org.json.JSONObject
import org.json.JSONArray
import java.util.UUID
import com.example.counter.domain.TimeUtils

data class Backup(val counters: List<CounterEntity>, val entries: List<EntryEntity>, val categories: List<CategoryEntity>)
object BackupCodec {
    fun encode(counters: List<CounterEntity>, entries: List<EntryEntity>, categories: List<CategoryEntity>): String {
        val root = JSONObject()
        val countersArray = JSONArray()
        counters.forEach { c ->
            countersArray.put(JSONObject().apply {
                put("id", c.id); put("name", c.name); put("color", c.color)
                put("icon", c.icon); put("step", c.step); put("initialValue", c.initialValue)
                put("target", c.target); put("category", c.category)
                put("sortOrder", c.sortOrder); put("archived", c.archived)
                put("createdAt", c.createdAt); put("pinned", c.pinned)
            })
        }
        val entriesArray = JSONArray()
        entries.forEach { e ->
            entriesArray.put(JSONObject().apply {
                put("id", e.id); put("counterId", e.counterId); put("delta", e.delta)
                put("valueAfter", e.valueAfter); put("occurredAt", e.occurredAt)
                put("localDate", e.localDate); put("tzOffsetSeconds", e.tzOffsetSeconds)
                put("note", e.note); put("createdAt", e.createdAt)
            })
        }
        val categoriesArray = JSONArray()
        categories.forEach { c ->
            categoriesArray.put(JSONObject().apply {
                put("name", c.name); put("sortOrder", c.sortOrder); put("color", c.color)
            })
        }
        root.put("version", 1)
        root.put("counters", countersArray)
        root.put("entries", entriesArray)
        root.put("categories", categoriesArray)
        return root.toString(2)
    }

    fun decode(text: String): Backup {
            val root = JSONObject(text)
            val countersArray = root.optJSONArray("counters") ?: error("缺少计数器数据")
            val entriesArray = root.optJSONArray("entries") ?: JSONArray()
            val categoriesArray = root.optJSONArray("categories") ?: JSONArray()

            val counters = (0 until countersArray.length()).map { i ->
                val o = countersArray.getJSONObject(i)
                CounterEntity(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    name = o.optString("name", "未命名"),
                    color = o.optLong("color", 0xFFF5921E),
                    icon = o.optString("icon", ""),
                    step = o.optDouble("step", 1.0),
                    initialValue = o.optDouble("initialValue", 0.0),
                    target = o.optDouble("target", 0.0),
                    category = o.optString("category", "默认"),
                    sortOrder = o.optInt("sortOrder", 0),
                    archived = o.optBoolean("archived", false),
                    createdAt = o.optLong("createdAt", 0L),
                    pinned = o.optBoolean("pinned", false)
                )
            }
            val entries = (0 until entriesArray.length()).map { i ->
                val o = entriesArray.getJSONObject(i)
                EntryEntity(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    counterId = o.optString("counterId", ""),
                    delta = o.optDouble("delta", 0.0),
                    valueAfter = o.optDouble("valueAfter", 0.0),
                    occurredAt = o.optLong("occurredAt", System.currentTimeMillis()),
                    localDate = o.optString("localDate", TimeUtils.todayYmd()),
                    tzOffsetSeconds = o.optInt("tzOffsetSeconds", 0),
                    note = o.optString("note", ""),
                    createdAt = o.optLong("createdAt", 0L)
                )
            }
            val categories = (0 until categoriesArray.length()).map { i ->
                val o = categoriesArray.getJSONObject(i)
                CategoryEntity(
                    name = o.optString("name", "默认"),
                    sortOrder = o.optInt("sortOrder", 0),
                    color = o.optLong("color", 0xFF9B9B9B)
                )
            }
            require(root.optInt("version", 1) == 1) { "备份版本不支持" }
            require(counters.map { it.id }.toSet().size == counters.size)
            require(entries.map { it.id }.toSet().size == entries.size)
            val ids = counters.map { it.id }.toSet()
            counters.forEach { require(it.id.isNotBlank() && it.name.isNotBlank() && it.step.isFinite() && it.step > 0 && it.initialValue.isFinite() && it.target.isFinite() && it.target >= 0) }
            entries.forEach { require(it.counterId in ids && it.delta.isFinite() && it.delta != 0.0); TimeUtils.parse(it.localDate); require(it.localDate == TimeUtils.ymdOfEpoch(it.occurredAt)) }
            return Backup(counters, entries, categories)
    }
}
