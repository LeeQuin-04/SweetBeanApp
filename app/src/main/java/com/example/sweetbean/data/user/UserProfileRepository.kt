package com.example.sweetbean.data.user

import com.example.sweetbean.core.UsernameValidator
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

sealed interface SetUsernameResult {
    data object Success : SetUsernameResult
    data object AlreadyTaken : SetUsernameResult
    data object ProfileNotFound : SetUsernameResult
}

class UserProfileRepository(
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val USERNAMES_COLLECTION = "usernames"
    }

    suspend fun getUserProfile(
        uid: String
    ): UserProfile? {
        require(uid.isNotBlank()) { "UID không được để trống." }

        val snapshot = firestore
            .collection(USERS_COLLECTION)
            .document(uid)
            .get()
            .await()

        return snapshot.toObject(UserProfile::class.java)
    }

    suspend fun hasUsername(
        uid: String
    ): Boolean {
        val profile = getUserProfile(uid)

        return !profile?.username.isNullOrBlank()
    }

    /**
     * Tạo hồ sơ users/{uid} nếu chưa có, với giá trị mặc định theo đặc tả
     * (role = USER, status = ACTIVE, các bộ đếm = 0, username để trống).
     *
     * Nếu hồ sơ đã có thì KHÔNG ghi đè gì cả, chỉ điền vào chỗ đang trống
     * (displayName, email). Nhờ vậy tên hiển thị mà người dùng đã tự sửa
     * không bị dữ liệu từ Google đè lên mỗi lần đăng nhập.
     */
    suspend fun ensureProfile(
        uid: String,
        email: String,
        displayName: String
    ) {
        require(uid.isNotBlank()) { "UID không được để trống." }

        val userRef = firestore
            .collection(USERS_COLLECTION)
            .document(uid)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)

            if (!snapshot.exists()) {
                transaction.set(
                    userRef,
                    mapOf(
                        "email" to email,
                        "displayName" to displayName,
                        "username" to "",
                        "usernameLower" to "",
                        "bio" to "",
                        "avatarUrl" to null,
                        "role" to UserRole.USER,
                        "status" to UserStatus.ACTIVE,
                        "followersCount" to 0,
                        "followingCount" to 0,
                        "postsCount" to 0,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                val updates = mutableMapOf<String, Any>()

                if (snapshot.getString("displayName").isNullOrBlank() && displayName.isNotBlank()) {
                    updates["displayName"] = displayName
                }

                if (snapshot.getString("email").isNullOrBlank() && email.isNotBlank()) {
                    updates["email"] = email
                }

                if (updates.isNotEmpty()) {
                    updates["updatedAt"] = FieldValue.serverTimestamp()
                    transaction.update(userRef, updates)
                }
            }
        }.await()
    }

    suspend fun isUsernameAvailable(
        username: String
    ): Boolean {
        val usernameLower = UsernameValidator.key(username)

        require(usernameLower.isNotBlank()) { "Username không được để trống." }

        val snapshot = firestore
            .collection(USERNAMES_COLLECTION)
            .document(usernameLower)
            .get()
            .await()

        return !snapshot.exists()
    }

    /**
     * Đặt hoặc đổi username bằng transaction (chống hai người cùng sở hữu một username).
     * - users/{uid}: lưu `username` (giữ nguyên chữ hoa/thường) và `usernameLower`.
     * - usernames/{usernameLower}: { userId, username }.
     * Nếu trước đó có username khác, chỉ mục cũ được xoá trong cùng transaction.
     * Đổi chữ hoa/thường của chính username đang dùng ("leequin" thành "LeeQuin") cũng hợp lệ.
     */
    suspend fun setUsername(
        uid: String,
        username: String
    ): SetUsernameResult {
        val displayUsername = UsernameValidator.clean(username)
        val usernameLower = UsernameValidator.key(username)

        require(uid.isNotBlank()) { "UID không được để trống." }
        require(UsernameValidator.isValid(displayUsername)) {
            "Username không hợp lệ."
        }

        val userRef = firestore
            .collection(USERS_COLLECTION)
            .document(uid)

        val usernameRef = firestore
            .collection(USERNAMES_COLLECTION)
            .document(usernameLower)

        return firestore.runTransaction { transaction ->
            // ĐỌC dữ liệu trước, chưa ghi bất cứ thứ gì (Firestore yêu cầu).
            val userSnapshot = transaction.get(userRef)

            if (!userSnapshot.exists()) {
                return@runTransaction SetUsernameResult.ProfileNotFound
            }

            val oldUsernameLower = userSnapshot
                .getString("usernameLower")
                ?.trim()
                ?.lowercase()

            val usernameSnapshot = transaction.get(usernameRef)

            if (usernameSnapshot.exists() && usernameSnapshot.getString("userId") != uid) {
                return@runTransaction SetUsernameResult.AlreadyTaken
            }

            // Nếu đang dùng một username khác, đọc chỉ mục cũ trước khi ghi.
            val oldUsernameRef = if (
                !oldUsernameLower.isNullOrBlank() && oldUsernameLower != usernameLower
            ) {
                firestore.collection(USERNAMES_COLLECTION)
                    .document(oldUsernameLower)
            } else {
                null
            }

            val oldUsernameSnapshot = oldUsernameRef?.let {
                transaction.get(it)
            }

            // Từ đây mới bắt đầu GHI dữ liệu.
            transaction.set(
                usernameRef,
                mapOf(
                    "userId" to uid,
                    "username" to displayUsername
                )
            )

            transaction.update(
                userRef,
                mapOf(
                    "username" to displayUsername,
                    "usernameLower" to usernameLower,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (
                oldUsernameRef != null &&
                oldUsernameSnapshot?.getString("userId") == uid
            ) {
                transaction.delete(oldUsernameRef)
            }

            SetUsernameResult.Success
        }.await()
    }
}