package com.example.engine3d.scene

open class Node(var name: String = "Node") {
    var parent: Node? = null
    val children = mutableListOf<Node>()
    private val signals = mutableMapOf<String, MutableList<(Any?) -> Unit>>()

    // Siklus Hidup Godot
    open fun _enter_tree() {}
    open fun _ready() {}
    open fun _process(delta: Double) {}
    open fun _physics_process(delta: Double) {}
    open fun _exit_tree() {}

    // Hierarki Pohon Objek
    fun add_child(child: Node) {
        child.parent = this
        children.add(child)
        child._enter_tree()
        child._ready()
    }

    fun remove_child(child: Node) {
        if (children.remove(child)) {
            child._exit_tree()
            child.parent = null
        }
    }

    fun get_parent(): Node? = parent
    fun get_children(): List<Node> = children

    fun find_child(searchName: String): Node? {
        if (this.name == searchName) return this
        for (child in children) {
            val found = child.find_child(searchName)
            if (found != null) return found
        }
        return null
    }

    // Sistem Sinyal Godot (Observer Pattern)
    fun connect(signal: String, callback: (Any?) -> Unit) {
        signals.getOrPut(signal) { mutableListOf() }.add(callback)
    }

    fun emit_signal(signal: String, arg: Any? = null) {
        signals[signal]?.forEach { it.invoke(arg) }
    }

    fun queue_free() {
        parent?.remove_child(this)
    }
}
