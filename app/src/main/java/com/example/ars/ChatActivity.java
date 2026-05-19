package com.example.ars;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.adapters.MessageAdapter;
import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.SupportMessage;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView rv;
    private MessageAdapter adapter;
    private List<SupportMessage> messageList = new ArrayList<>();
    private EditText etInput;
    private ApiService apiService;

    private int requestId;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        apiService = RetrofitClient.getApiService();

        requestId = getIntent().getIntExtra("REQUEST_ID", -1);
        currentUserId = getIntent().getIntExtra("USER_ID", -1);
        String subject = getIntent().getStringExtra("SUBJECT");
        String content = getIntent().getStringExtra("CONTENT");

        ((TextView)findViewById(R.id.tvChatSubject)).setText(subject);
        ((TextView)findViewById(R.id.tvChatDescription)).setText(content);

        rv = findViewById(R.id.rvChatMessages);
        etInput = findViewById(R.id.etMessageInput);
        ImageButton btnSend = findViewById(R.id.btnSendMessage);

        rv.setLayoutManager(new LinearLayoutManager(this));

        btnSend.setOnClickListener(v -> sendMessage());

        loadMessages();
    }

    private void loadMessages() {
        apiService.getChatMessages(requestId).enqueue(new Callback<List<SupportMessage>>() {
            @Override
            public void onResponse(Call<List<SupportMessage>> call, Response<List<SupportMessage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    messageList = response.body();
                    adapter = new MessageAdapter(messageList, currentUserId);
                    rv.setAdapter(adapter);
                    rv.scrollToPosition(messageList.size() - 1);
                }
            }
            @Override
            public void onFailure(Call<List<SupportMessage>> call, Throwable t) {}
        });
    }

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;

        SupportMessage message = new SupportMessage(requestId, currentUserId, text);
        apiService.sendChatMessage(message).enqueue(new Callback<SupportMessage>() {
            @Override
            public void onResponse(Call<SupportMessage> call, Response<SupportMessage> response) {
                if (response.isSuccessful()) {
                    etInput.setText("");
                    loadMessages();
                }
            }
            @Override
            public void onFailure(Call<SupportMessage> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }
}