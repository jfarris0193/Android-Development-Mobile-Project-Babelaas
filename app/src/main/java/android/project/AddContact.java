package android.project;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;


public class AddContact extends AppCompatActivity {

    private Handler handler;

    private static int LOAD_IMAGE = 1;
    private ImageButton uploadImage;

    private String imagePath = "";
    private String name;
    private String number;
    private String image;

    private ProgressDialog progressDialog;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;

    MapView gmap;
    GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        handler = new Handler(getApplicationContext());

        uploadImage = (ImageButton) findViewById(R.id.uploadContactImage);
        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });


        Button createContactButton = (Button) findViewById(R.id.createContactButton);
        createContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText getContactName = (EditText) findViewById(R.id.getContactName);
                name = getContactName.getText().toString();
                EditText getContactNumber = (EditText) findViewById(R.id.getContactNumber);
                number = getContactNumber.getText().toString();

                ContactInformation contactInformation = new ContactInformation();
                contactInformation.setName(name);
                contactInformation.setNumber(number);

                Boolean added = handler.addContact(contactInformation);
                if (added) {
                    Intent myIntent = new Intent(AddContact.this, Contact_ListView.class);
                    startActivity(myIntent);
                } else {
                    Toast.makeText(getApplicationContext(), "Couldn't Create Contact", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    //creates pop up box with photo upload options
    protected void selectImage() {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder addPhoto = new AlertDialog.Builder(AddContact.this);
        addPhoto.setTitle("Add Photo:");
        addPhoto.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (options[which].equals("Take Photo")) {
                    activeTakePhoto();
                } else if (options[which].equals("Choose from Gallery")) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, 2);
                } else if (options[which].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        addPhoto.show();
    }

    //checks if permission to use phones camera and storage is already allowed
    private void activeTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 110);
        } else {
            takePicture();
        }
    }

    //launches functionality for taking/uploading a new photo
    public void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 1);
    }

    //asks user if app can access the phones camera and storage if not allowed
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 110) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                takePicture();
            }
        }
    }

    //functionality for taking/uploading a new photo
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) { //if wanting to take a new picture
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                uploadImage.setImageBitmap(Bitmap.createScaledBitmap(bitmap, uploadImage.getWidth(), uploadImage.getHeight(), false));

            } else if (requestCode == 2) { //if wanting to upload image from phone storage
                Uri selectedImage = data.getData();
                String[] filePath = {MediaStore.Images.Media.DATA};
                Cursor c = getContentResolver().query(selectedImage, filePath, null, null, null);
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePath[0]);
                String picturePath = c.getString(columnIndex);
                c.close();
                Bitmap thumbnail = (BitmapFactory.decodeFile(picturePath));
                uploadImage.setImageBitmap(Bitmap.createScaledBitmap(thumbnail, uploadImage.getWidth(), uploadImage.getHeight(), false));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(1, 1, 0, "HomePage");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem items) {
        int iden = items.getItemId();

        switch (iden) {
            case 1:
                Intent myIntent = new Intent(AddContact.this, HomePage.class);
                startActivity(myIntent);
                break;

        }
        return super.onOptionsItemSelected(items);
    }
}




