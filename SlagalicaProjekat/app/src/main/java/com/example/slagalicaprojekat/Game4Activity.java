package com.example.slagalicaprojekat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game4Activity extends AppCompatActivity {

    private DatabaseReference databaseReference;
    private List<Button> buttonList;
    private int currentButtonIndex;
    private KorakPoKorak selectedObject;

    private CountDownTimer timer;
    private TextView timerTextView;

    private TextView pointsTextView;
    private final long TIMER_DURATION = 11000; // 10 seconds

    private int totalPoints;
    private boolean isPointsDialogShown = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game4);

        FirebaseApp.initializeApp(this);
        resetPoints();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        buttonList = new ArrayList<>();
        buttonList.add(findViewById(R.id.btnTerm1));
        buttonList.add(findViewById(R.id.btnTerm2));
        buttonList.add(findViewById(R.id.btnTerm3));
        buttonList.add(findViewById(R.id.btnTerm4));
        buttonList.add(findViewById(R.id.btnTerm5));
        buttonList.add(findViewById(R.id.btnTerm6));
        buttonList.add(findViewById(R.id.btnTerm7));

        timerTextView = findViewById(R.id.tajmer);
        pointsTextView = findViewById(R.id.tvScore);

        // Read the data from the database
        readDataFromFirebase();
    }


    private void readDataFromFirebase() {
        // Assuming your data is stored under the "objects" node in your Firebase database
        DatabaseReference objectsRef = databaseReference.child("KorakPoKorak");

        // Attach a listener to retrieve the data once
        objectsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<KorakPoKorak> objectList = new ArrayList<>();

                    // Iterate through the children of the "objects" node
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        // Get the data for each child and convert it to ObjectModel
                        KorakPoKorak object = snapshot.getValue(KorakPoKorak.class);
                        if (object != null) {
                            objectList.add(object);
                        }
                    }

                    // Choose a random object from the list
                    Random random = new Random();
                    int randomIndex = random.nextInt(objectList.size());
                    selectedObject = objectList.get(randomIndex);

                    // Start updating the buttons
                    startButtonUpdates();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle any errors that occur
                Log.e("Firebase", "Error retrieving data from Firebase: " + databaseError.getMessage());
            }
        });
    }
    private void startButtonUpdates() {
        currentButtonIndex = 0;
        updateButton();
    }


    private void updateButton() {
        if (currentButtonIndex < buttonList.size()) {
            Button currentButton = buttonList.get(currentButtonIndex);

            String propertyName = "korak" + (currentButtonIndex + 1);
            String propertyValue = getProperty(selectedObject, propertyName);

            currentButton.setText(propertyValue);

            currentButtonIndex++;

            EditText solutionEditText = findViewById(R.id.etSolution);



            solutionEditText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_NULL) {
                    String userInput = v.getText().toString();
                    if (userInput.equalsIgnoreCase(selectedObject.getResenje())) {
                        stopButtonUpdates();
                        int currentButtonIndex = getCurrentButtonIndex();
                        int points = 22 - currentButtonIndex * 2;
                        showPointsDialog(points);
                    }else {
                        showWrongSolutionAlert();
                        renderNextButton();
                    }
                    return true;
                }
                return false;
            });


            if (currentButtonIndex >= buttonList.size()) {

                solutionEditText.setText(selectedObject.getResenje());
            } else {

                startTimer();
            }
        }
    }

    private void showWrongSolutionAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pogresno resenje");
        builder.setMessage("Pogresno resenje.");
        builder.setPositiveButton("OK", (dialog, which) -> {
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void renderNextButton() {
        if (currentButtonIndex < buttonList.size()) {
            Button currentButton = buttonList.get(currentButtonIndex);

            String propertyName = "step" + (currentButtonIndex + 1);
            String propertyValue = getProperty(selectedObject, propertyName);

            currentButton.setText(propertyValue);

            currentButtonIndex++;

            startTimer();
        }
    }
    private void stopButtonUpdates() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private int getCurrentButtonIndex() {
        return currentButtonIndex;
    }

    private void startTimer() {
        if (timer != null) {
            timer.cancel();
        }

        timer = new CountDownTimer(TIMER_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                timerTextView.setText(String.valueOf(secondsRemaining));
            }

            @Override
            public void onFinish() {
                updateButton();
            }
        };

        timer.start();
    }

    private String getProperty(Object object, String propertyName) {
        try {
            Field field = object.getClass().getDeclaredField(propertyName);
            field.setAccessible(true);
            Object value = field.get(object);
            if (value != null) {
                return value.toString();
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void showPointsDialog(int points) {
        if (isPointsDialogShown) {
            return; // Ako je dijalog već prikazan, izlazimo iz metode
        }

        isPointsDialogShown = true;

        savePoints(points);
        int totalPoints = getTotalPoints();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Bodovi");
        builder.setMessage("Osvojili ste " + points + " bodova!");
        builder.setPositiveButton("OK", (dialog, which) -> {
            isPointsDialogShown = false; // Dijalog je zatvoren, dozvoljavamo prikazivanje sledećeg dijaloga
            // Handle the OK button click if needed
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void savePoints(int points) {
        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        this.totalPoints = preferences.getInt("totalPoints", 0);
        this.totalPoints += points;
        pointsTextView.setText("Bodovi: " + String.valueOf(this.totalPoints));
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("totalPoints", this.totalPoints);
        editor.apply();
    }

    private int getTotalPoints() {
        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        return preferences.getInt("totalPoints", 0);
    }

    private void resetPoints() {
        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("totalPoints", 0);
        editor.apply();
    }


}