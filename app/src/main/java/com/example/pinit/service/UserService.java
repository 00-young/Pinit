package com.example.pinit.service;


import com.example.pinit.model.UserPreference;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserService {

    public interface UserCallback {

        void onSuccess(
                UserPreference user
        );

        void onFailure(
                String error
        );
    }

    public void getUserPreference(

            UserCallback callback
    ) {
        String uid = "testUser";
   /*     String uid =

                FirebaseAuth
                        .getInstance()
                        .getCurrentUser()
                        .getUid();  */

        FirebaseFirestore db =
                FirebaseFirestore
                        .getInstance();

        db.collection("users")
                .document(uid)

                .get()

                .addOnSuccessListener(

                        documentSnapshot -> {

                            if (
                                    documentSnapshot.exists()
                            ) {

                                UserPreference user =

                                        documentSnapshot
                                                .toObject(
                                                        UserPreference.class
                                                );

                                callback.onSuccess(
                                        user
                                );
                            }
                        }
                )

                .addOnFailureListener(

                        e -> {

                            callback.onFailure(
                                    e.getMessage()
                            );
                        }
                );
    }
}