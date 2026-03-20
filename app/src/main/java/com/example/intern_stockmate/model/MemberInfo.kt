package com.example.intern_stockmate.model

data class MemberInfo(
    val totalMembers: Int = 0,
    val activeMembers: Int = 0,
    val inactiveMembers: Int = 0,
    val maleCount: Int = 0,
    val femaleCount: Int = 0,
    val birthdayThisMonth: Int = 0,
    val lastUpdate: String = ""
)