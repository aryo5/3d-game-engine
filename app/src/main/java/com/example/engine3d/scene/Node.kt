package com.example.engine3d.scene

import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import com.example.engine3d.script.SceneTreeManager

open class Node(var name: String = "Node") {
    var parent: Node? = null
    val children = mutableListOf<Node>()

    private val luaSignals = mutableMapOf<String, MutableList<LuaValue>>()

    var isProcessing: Boolean = false
        private set

    open fun _enter_tree() {}
    open fun _ready() {}
    open fun _process(delta: Double) {}
    open fun _physics_process(delta: Double) {}
    open fun _exit_tree() {}

    fun set_process(enable: Boolean) {
        isProcessing = enable
        if (enable) {
            SceneTreeManager.tree.registerProcessingNode(this)
        } else {
            SceneTreeManager.tree.unregisterProcessingNode(this)
        }
    }
    fun setProcess(enable: Boolean) = set_process(enable)

    fun connect(signal: String, luaCallback: LuaValue) {
        luaSignals.getOrPut(signal) { mutableListOf() }.add(luaCallback)
    }

    fun emit_signal(signal: String, vararg args: Any?) {
        luaSignals[signal]?.forEach { callback ->
            try {
                if (args.isEmpty()) {
                    callback.call()
                } else {
                    val luaArgs = args.map { CoerceJavaToLua.coerce(it) }.toTypedArray()
                    callback.invoke(LuaValue.varargsOf(luaArgs))
                }
            } catch (e: Throwable) {
                android.util.Log.e("Apex3D-Signal", "Error sinyal $signal: ${e.message}")
            }
        }
    }
    fun emitSignal(signal: String, vararg args: Any?) = emit_signal(signal, *args)

    fun add_child(child: Node) {
        child.parent = this
        children.add(child)
        child._enter_tree()
        child._ready()
    }

    fun remove_child(child: Node) {
        if (children.remove(child)) {
            child.set_process(false)
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

    fun queue_free() {
        parent?.remove_child(this)
    }
}
