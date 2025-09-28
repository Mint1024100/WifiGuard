package com.wifiguard.core.common

interface Mapper<in From, out To> {
    fun map(from: From): To
}

interface ListMapper<in From, out To> : Mapper<List<From>, List<To>> {
    fun mapItem(from: From): To
    
    override fun map(from: List<From>): List<To> {
        return from.map { mapItem(it) }
    }
}

abstract class BaseMapper<in From, out To> : Mapper<From, To>
abstract class BaseListMapper<in From, out To> : ListMapper<From, To>