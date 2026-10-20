package com.example.navigationstepbysteptutorial

object Routes {
    const val HOME = "home"
    const val DETAIL = "detail/{itemId}"   // {itemId} is a placeholder for an argument
    const val SETTINGS = "settings"

    // Builds the real route for a given item
    fun detail(itemId: Int) = "detail/$itemId"
}