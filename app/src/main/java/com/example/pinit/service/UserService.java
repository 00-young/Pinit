package com.example.pinit.service;


import com.example.pinit.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserService {

    public interface UserCallback {

        void onSuccess(
                User user
        );

        void onFailure(
                String error
        );
    }

    public void getUser(

            UserCallback callback
    ) {
        String email =

                FirebaseAuth
                        .getInstance()
                        .getCurrentUser()
                        .getEmail();

        FirebaseFirestore db =
                FirebaseFirestore
                        .getInstance();

        db.collection("users")
                .document(email)

                .get()

                .addOnSuccessListener(

                        documentSnapshot -> {

                            if (
                                    documentSnapshot.exists()
                            ) {

                                User user =

                                        documentSnapshot
                                                .toObject(
                                                        User.class
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