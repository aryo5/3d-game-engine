package com.example.engine3d.scene

class SceneTree {
    val root: Node = Node("Root")
    var currentScene: Node? = null

    // Panggil _process(delta) ke seluruh node di pohon
    fun process(delta: Double) {
        propagateProcess(root, delta)
    }

    // Panggil _physics_process(delta) ke seluruh node di pohon
    fun physics_process(delta: Double) {
        propagatePhysicsProcess(root, delta)
    }

    private fun propagateProcess(node: Node, delta: Double) {
        node._process(delta)
        for (child in node.children) {
            propagateProcess(child, delta)
        }
    }

    private fun propagatePhysicsProcess(node: Node, delta: Double) {
        node._physics_process(delta)
        for (child in node.children) {
            propagatePhysicsProcess(child, delta)
        }
    }

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
