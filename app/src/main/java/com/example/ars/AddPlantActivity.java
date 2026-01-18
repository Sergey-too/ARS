package com.example.ars;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.Category;
import com.example.ars.models.Crop;
import com.example.ars.models.Region;
import com.example.ars.utils.SharedPreferencesHelper;
import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddPlantActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;

    private ApiService apiService;
    private SharedPreferencesHelper prefsHelper;

    private List<Category> categories = new ArrayList<>();
    private List<Crop> cropsByCategory = new ArrayList<>();
    private List<Region> regions = new ArrayList<>();

    private Integer selectedCategoryId = null;  // Храним ID выбранной категории
    private String selectedCategoryName = null; // И название
    private Integer selectedCropId = null;
    private String selectedRegionName = null;

    private Uri selectedImageUri;
    private String uploadedImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plant);

        prefsHelper = new SharedPreferencesHelper(this);
        apiService = RetrofitClient.getApiService();

        // Кнопка назад
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> showExitDialog());

        // Настройка выбора фото
        setupPhotoSelection();

        // Загружаем данные из БД
        loadCategories();
        loadRegions();

        // Настраиваем слушатели
        setupDropdownListeners();

        // Кнопка добавления растения
        MaterialButton btnAddPlant = findViewById(R.id.btnAddPlant);
        btnAddPlant.setOnClickListener(v -> addPlantToUser());
    }

    private void setupPhotoSelection() {
        CardView photoCard = findViewById(R.id.photoCard);
        photoCard.setOnClickListener(v -> showImagePickerDialog());
    }

    private void showImagePickerDialog() {
        String[] options = {"Камера", "Галерея", "Отмена"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите источник фото");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) openCamera();
            else if (which == 1) openGallery();
        });
        builder.show();
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        } else {
            Toast.makeText(this, "Камера недоступна", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                selectedImageUri = data.getData();
                showSelectedImage();
                if (selectedCategoryName != null) {
                    uploadCropImageToServer();
                }
            } else if (requestCode == CAMERA_REQUEST && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    android.graphics.Bitmap photo = (android.graphics.Bitmap) extras.get("data");
                    if (photo != null) {
                        selectedImageUri = saveBitmapToFile(photo);
                        showSelectedImage();
                        if (selectedCategoryName != null) {
                            uploadCropImageToServer();
                        }
                    }
                }
            }
        }
    }

    private Uri saveBitmapToFile(android.graphics.Bitmap bitmap) {
        File file = new File(getCacheDir(), "temp_plant_photo.jpg");
        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream(file);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            return Uri.fromFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showSelectedImage() {
        ImageView ivPlantPhoto = findViewById(R.id.ivPlantPhoto);
        if (selectedImageUri != null) {
            Picasso.get()
                    .load(selectedImageUri)
                    .placeholder(R.drawable.ic_add)
                    .error(R.drawable.ic_plant_error)
                    .into(ivPlantPhoto);
        }
    }

    // ЗАГРУЗКА КАТЕГОРИЙ ИЗ БД
    private void loadCategories() {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categories = response.body();
                    updateCategoryDropdown();
                    Log.d("CATEGORIES", "Loaded " + categories.size() + " categories");
                } else {
                    Toast.makeText(AddPlantActivity.this,
                            "Категории не найдены", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(AddPlantActivity.this,
                        "Ошибка загрузки категорий: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ЗАГРУЗКА РЕГИОНОВ ИЗ БД
    private void loadRegions() {
        apiService.getRegions().enqueue(new Callback<List<Region>>() {
            @Override
            public void onResponse(Call<List<Region>> call, Response<List<Region>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    regions = response.body();
                    updateRegionDropdown();
                } else {
                    Toast.makeText(AddPlantActivity.this,
                            "Регионы не найдены", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Region>> call, Throwable t) {
                Toast.makeText(AddPlantActivity.this,
                        "Ошибка загрузки регионов: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCategoryDropdown() {
        AutoCompleteTextView actvPlantType = findViewById(R.id.actvPlantType);
        List<String> categoryNames = new ArrayList<>();
        for (Category category : categories) {
            categoryNames.add(category.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, categoryNames);
        actvPlantType.setAdapter(adapter);
    }

    private void updateRegionDropdown() {
        AutoCompleteTextView actvRegion = findViewById(R.id.actvRegion);
        List<String> regionNames = new ArrayList<>();
        for (Region region : regions) {
            regionNames.add(region.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, regionNames);
        actvRegion.setAdapter(adapter);
    }

    private void setupDropdownListeners() {
        AutoCompleteTextView actvPlantType = findViewById(R.id.actvPlantType);
        AutoCompleteTextView actvPlantName = findViewById(R.id.actvPlantName);
        AutoCompleteTextView actvRegion = findViewById(R.id.actvRegion);
        TextView tvDescription = findViewById(R.id.tvPlantDescription);

        actvPlantType.setOnItemClickListener((parent, view, position, id) -> {
            if (position < categories.size()) {
                selectedCategoryName = categories.get(position).getName();

                Log.d("CATEGORY", "Выбрана категория: " + selectedCategoryName);

                actvPlantName.setText("");
                tvDescription.setText("Выберите растение чтобы увидеть описание");

                loadCropsByCategoryName(selectedCategoryName);
            }
        });

        actvPlantName.setOnItemClickListener((parent, view, position, id) -> {
            if (position < cropsByCategory.size()) {
                Crop selectedCrop = cropsByCategory.get(position);
                selectedCropId = selectedCrop.getId();

                Log.d("CROP", "Выбрано растение: " + selectedCrop.getName() + " (ID: " + selectedCropId + ")");

                // Показываем описание растения
                if (selectedCrop.getDescription() != null && !selectedCrop.getDescription().isEmpty()) {
                    tvDescription.setText(selectedCrop.getDescription());
                } else {
                    tvDescription.setText("Описание отсутствует");
                }
            }
        });

        // При выборе региона
        actvRegion.setOnItemClickListener((parent, view, position, id) -> {
            if (position < regions.size()) {
                selectedRegionName = regions.get(position).getName();
                Log.d("REGION", "Выбран регион: " + selectedRegionName);
            }
        });
    }

    private void loadCropsByCategoryName(String categoryName) {
        Log.d("CROP_LOAD", "Загрузка растений для категории: " + categoryName);

        // Используйте правильный метод - getCropsByCategory, а не getCropsByCategoryName
        apiService.getCropsByCategory(categoryName).enqueue(new Callback<List<Crop>>() {
            @Override
            public void onResponse(Call<List<Crop>> call, Response<List<Crop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cropsByCategory = response.body();
                    Log.d("CROP_LOAD", "Загружено " + cropsByCategory.size() + " растений");
                    updateCropNamesDropdown();
                } else {
                    Log.e("CROP_LOAD", "Ошибка: " + response.code());
                    Toast.makeText(AddPlantActivity.this,
                            "Растения не найдены", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Crop>> call, Throwable t) {
                Log.e("CROP_LOAD", "Ошибка: " + t.getMessage());
                Toast.makeText(AddPlantActivity.this,
                        "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCropNamesDropdown() {
        AutoCompleteTextView actvPlantName = findViewById(R.id.actvPlantName);
        List<String> cropNames = new ArrayList<>();
        for (Crop crop : cropsByCategory) {
            cropNames.add(crop.getName());
            Log.d("CROPS", "Crop: " + crop.getName() + " (ID: " + crop.getId() + ")");
        }
        ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, cropNames);
        actvPlantName.setAdapter(nameAdapter);
    }
    private String getCategoryFolder(String category) {
        if (category == null) return "default";

        switch(category.toLowerCase()) {
            case "цветы":
            case "flowers":
                return "flowers";
            case "фрукты":
            case "fruits":
                return "fruits";
            case "овощи":
            case "vegetables":
                return "vegetables";
            case "деревья":
            case "woods":
            case "trees":
                return "woods";
            default:
                return "default";
        }
    }
    private void uploadCropImageToServer() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Сначала выберите фото", Toast.LENGTH_SHORT).show();
            return;
        }

        // Получаем название категории для папки
        String categoryFolder = "default";
        if (selectedCategoryName != null) {
            categoryFolder = getCategoryFolder(selectedCategoryName);
        }

        String filePath = getRealPathFromURI(selectedImageUri);
        if (filePath == null) {
            Toast.makeText(this, "Не удалось получить файл", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(filePath);
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);

        // Используем оригинальное имя файла или генерируем новое
        String fileName = "plant_" + System.currentTimeMillis() + ".jpg";
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, requestFile);

        // Отправляем категорию для определения папки
        RequestBody categoryBody = RequestBody.create(MediaType.parse("text/plain"), categoryFolder);

        apiService.uploadCropImage(body, categoryBody).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    uploadedImageUrl = response.body();
                    Toast.makeText(AddPlantActivity.this, "Фото загружено успешно", Toast.LENGTH_SHORT).show();
                    // Обновляем изображение в UI
                    loadPlantImageFromServer(uploadedImageUrl);
                } else {
                    Toast.makeText(AddPlantActivity.this,
                            "Ошибка загрузки: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(AddPlantActivity.this,
                        "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void loadPlantImageFromServer(String imageUrl) {
        ImageView imageView = findViewById(R.id.ivPlantPhoto);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String fullUrl = RetrofitClient.BASE_URL + imageUrl;
            Picasso.get()
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_plant_placeholder)
                    .error(R.drawable.ic_plant_error)
                    .into(imageView);
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        android.database.Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        return contentUri.getPath();
    }

    private void addPlantToUser() {
        com.example.ars.models.User currentUser = prefsHelper.getUser();
        if (currentUser == null || currentUser.getId() == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategoryName == null) {
            Toast.makeText(this, "Выберите категорию растения", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCropId == null) {
            Toast.makeText(this, "Выберите растение", Toast.LENGTH_SHORT).show();
            return;
        }

        // Проверяем что регион выбран (для отображения, но не отправляем в БД)
        if (selectedRegionName == null) {
            Toast.makeText(this, "Выберите регион", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("ADD_PLANT", "Добавление растения: userId=" + currentUser.getId() +
                ", cropId=" + selectedCropId + ", category=" + selectedCategoryName +
                ", region=" + selectedRegionName);

        // Создаем запрос (ТОЛЬКО обязательные поля)
        Map<String, Object> request = new HashMap<>();
        request.put("userId", currentUser.getId());
        request.put("cropId", selectedCropId);

        Log.d("ADD_PLANT", "Отправка запроса на /api/crops/user/add: " + request);

        apiService.addUserCrop(request).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Log.d("ADD_PLANT", "Ответ от сервера: код " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    Log.d("ADD_PLANT", "Тело ответа: " + result);

                    Boolean success = (Boolean) result.get("success");

                    if (success != null && success) {
                        Toast.makeText(AddPlantActivity.this, "Растение добавлено!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(AddPlantActivity.this, PlantsActivity.class));
                        finish();
                    } else {
                        String error = (String) result.get("error");
                        Toast.makeText(AddPlantActivity.this, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Выводим больше информации об ошибке
                    try {
                        String errorBody = response.errorBody() != null ?
                                response.errorBody().string() : "No error body";
                        Log.e("ADD_PLANT", "Error body: " + errorBody);
                        Log.e("ADD_PLANT", "Error code: " + response.code());
                        Log.e("ADD_PLANT", "Error message: " + response.message());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(AddPlantActivity.this,
                            "Ошибка сервера: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("ADD_PLANT", "Network error: " + t.getMessage());
                t.printStackTrace();
                Toast.makeText(AddPlantActivity.this,
                        "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Вернуться назад?")
                .setMessage("Все несохраненные данные будут потеряны. Вы уверены?")
                .setPositiveButton("Да", (dialog, which) -> {
                    startActivity(new Intent(this, PlantsActivity.class));
                    finish();
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        showExitDialog();
    }
}