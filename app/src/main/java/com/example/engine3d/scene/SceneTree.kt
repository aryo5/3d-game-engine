package com.example.engine3d.scene

class SceneTree {
    val root: Node = Node("Root")
    var currentScene: Node? = null

    // Antrean Khusus: HANYA berisi node yang aktif memanggil set_process(true)
    private val activeNodes = mutableSetOf<Node>()

    fun registerProcessingNode(node: Node) {
        activeNodes.add(node)
    }

    fun unregisterProcessingNode(node: Node) {
        activeNodes.remove(node)
    }

    // Eksekusi cepat: HANYA mengulang node yang aktif di antrean (0% CPU untuk yang tidur!)
    fun process(delta: Double) {
        for (node in activeNodes) {
            node._process(delta)
        }
    }

    fun physics_process(delta: Double) {
        for (node in activeNodes) {
            node._physics_process(delta)
        }
    }

    // Statistik Efisiensi Memori
    fun getActiveCount(): Int = activeNodes.size
    fun getTotalCount(): Int = get_all_nodes().size

    fun get_all_nodes(): List<Node> {
        val list = mutableListOf<Node>()
        collectNodes(root, list)
        return list
    }

    private fun collectNodes(node: Node, list: MutableList<Node>) {
        list.add(node)
        for (child in node.children) {
            collectNodes(child, list)
        }
    }
}
