package com.xlythe.floatingview

import java.util.LinkedList

class LimitedQueue<E>(private val limit: Int) : LinkedList<E?>() {
    override fun add(o: E?): Boolean {
        super.add(o)
        while (size > limit) {
            super.remove()
        }
        return true
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
