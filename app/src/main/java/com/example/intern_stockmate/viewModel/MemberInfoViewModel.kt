package com.example.intern_stockmate.viewModel

import androidx.lifecycle.ViewModel
import com.example.intern_stockmate.model.MemberInfo
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MemberInfoViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _memberState = MutableStateFlow<MemberInfoUiState>(MemberInfoUiState.Loading)
    val memberState: StateFlow<MemberInfoUiState> = _memberState.asStateFlow()

    init {
        fetchMemberSummary()
    }

    fun fetchMemberSummary() {
        _memberState.value = MemberInfoUiState.Loading

        firestore.collection(COLLECTION_NAME)
            .document(DOCUMENT_NAME)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    _memberState.value = MemberInfoUiState.Error(
                        "Member summary document was not found."
                    )
                    return@addOnSuccessListener
                }

                val memberInfo = MemberInfo(
                    totalMembers = snapshot.intValue("TotalMembers"),
                    activeMembers = snapshot.intValue("ActiveMembers"),
                    inactiveMembers = snapshot.intValue("InactiveMembers"),
                    maleCount = snapshot.intValue("MaleCount"),
                    femaleCount = snapshot.intValue("FemaleCount"),
                    birthdayThisMonth = snapshot.intValue("BirthdayThisMonth"),
                    lastUpdate = snapshot.stringValue("lastUpdate")
                )

                _memberState.value = MemberInfoUiState.Success(memberInfo)
            }
            .addOnFailureListener { error ->
                _memberState.value = MemberInfoUiState.Error(
                    error.message ?: "Failed to load member info from Firestore."
                )
            }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.intValue(key: String): Int {
        val value = get(key) ?: return 0
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: 0
            else -> 0
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.stringValue(key: String): String =
        get(key)?.toString().orEmpty()

    private companion object {
        const val COLLECTION_NAME = "MemberSummary"
        const val DOCUMENT_NAME = "Current"
    }
}

sealed interface MemberInfoUiState {
    data object Loading : MemberInfoUiState
    data class Success(val data: MemberInfo) : MemberInfoUiState
    data class Error(val message: String) : MemberInfoUiState
}