package com.example.sqlite;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sqlite.adapter.EtudiantAdapter;
import com.example.sqlite.classe.Etudiant;
import com.example.sqlite.service.EtudiantService;
import com.example.sqlite.utile.ImageUtil;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TableActivity extends AppCompatActivity implements EtudiantAdapter.OnEtudiantListener {
    private static final int REQUEST_IMAGE_PICK = 1;

    private RecyclerView recyclerView;
    private EtudiantAdapter adapter;
    private EtudiantService etudiantService;
    private List<Etudiant> etudiantList;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private Calendar calendar = Calendar.getInstance();

    private ImageView editImageView;
    private String selectedImagePath;
    private AlertDialog currentDialog;
    private Etudiant currentEditingEtudiant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table);

        etudiantService = new EtudiantService(this);

        etudiantList = etudiantService.findAll();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EtudiantAdapter(this, etudiantList, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onEtudiantClick(int position) {
        Etudiant etudiant = etudiantList.get(position);
        showOptionsDialog(etudiant, position);
    }

    private void showOptionsDialog(final Etudiant etudiant, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Options pour " + etudiant.getNom() + " " + etudiant.getPrenom());

        String[] options = {"Modifier", "Supprimer"};

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showEditDialog(etudiant);
            } else {
                showDeleteConfirmation(etudiant, position);
            }
        });

        builder.setNegativeButton("Annuler", null);

        builder.create().show();
    }

    private void showEditDialog(final Etudiant etudiant) {
        currentEditingEtudiant = etudiant;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modifier Étudiant");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_etudiant, null);
        final EditText editNom = view.findViewById(R.id.edit_nom);
        final EditText editPrenom = view.findViewById(R.id.edit_prenom);
        final TextView txtDateNaissance = view.findViewById(R.id.edit_date_naissance);
        final Button btnSelectDate = view.findViewById(R.id.btn_edit_select_date);
        editImageView = view.findViewById(R.id.edit_image_etudiant);
        Button btnSelectImage = view.findViewById(R.id.btn_select_image);

        editNom.setText(etudiant.getNom());
        editPrenom.setText(etudiant.getPrenom());

        Date selectedDate = etudiant.getDateNaissance();
        if (selectedDate != null) {
            txtDateNaissance.setText(dateFormat.format(selectedDate));
            calendar.setTime(selectedDate);
        } else {
            txtDateNaissance.setText("Non sélectionnée");
        }

        selectedImagePath = etudiant.getImagePath();
        if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
            Bitmap bitmap = ImageUtil.loadBitmapFromPath(selectedImagePath);
            if (bitmap != null) {
                editImageView.setImageBitmap(bitmap);
            } else {
                editImageView.setImageResource(R.drawable.ic_person_placeholder); // Placeholder in case of error
            }
        } else {
            editImageView.setImageResource(R.drawable.ic_person_placeholder); // Placeholder if no image path
        }


        btnSelectDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    TableActivity.this,
                    (view1, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        txtDateNaissance.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        });

        builder.setView(view);

        builder.setPositiveButton("Enregistrer", (dialog, which) -> {
            String nom = editNom.getText().toString().trim();
            String prenom = editPrenom.getText().toString().trim();

            if (!nom.isEmpty() && !prenom.isEmpty()) {
                etudiant.setNom(nom);
                etudiant.setPrenom(prenom);
                etudiant.setDateNaissance(calendar.getTime());
                etudiant.setImagePath(selectedImagePath);
                etudiantService.update(etudiant);

                refreshList();

                Toast.makeText(TableActivity.this, "Étudiant modifié avec succès", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(TableActivity.this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Annuler", null);

        currentDialog = builder.create();
        currentDialog.show();
    }

    private void showDeleteConfirmation(final Etudiant etudiant, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Supprimer Étudiant");
        builder.setMessage("Êtes-vous sûr de vouloir supprimer cet étudiant ?");

        builder.setPositiveButton("Oui", (dialog, which) -> {
            etudiantService.delete(etudiant);
            refreshList();
            Toast.makeText(TableActivity.this, "Étudiant supprimé avec succès", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Non", null);

        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            selectedImagePath = ImageUtil.saveImageToPrivateStorage(this, selectedImageUri);

            try {
                Bitmap bitmap = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), selectedImageUri));
                }
                editImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void refreshList() {
        etudiantList.clear();
        etudiantList.addAll(etudiantService.findAll());
        adapter.notifyDataSetChanged();
    }
}
