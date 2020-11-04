package com.android.veapp.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.android.veapp.R;
import com.android.veapp.data.SharedPreferenceHelper;
import com.android.veapp.data.StaticConfig;
import com.android.veapp.model.Consersation;
import com.android.veapp.model.Message;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

//Clase utilizada para todo lo relacionado al chat
public class ChatActivity extends AppCompatActivity implements View.OnClickListener {
    private RecyclerView recyclerChat;
    public static final int VIEW_TYPE_USER_MESSAGE = 0;
    public static final int VIEW_TYPE_FRIEND_MESSAGE = 1;
    private ListMessageAdapter adapter;
    private String roomId;
    private ArrayList<CharSequence> idFriend;
    private Consersation consersation;
    private ImageButton btnSend;
    private EditText editWriteMessage;
    private LinearLayoutManager linearLayoutManager;
    public static HashMap<String, Bitmap> bitmapAvataFriend;
    public Bitmap bitmapAvataUser;
    private MenuItem camButton;
    private FloatingActionButton alertButton;
    private MenuItem finAlertButton;
    private FloatingActionButton camaraF;
    private static final String TAG = "MainActivity";


    //Metodo con intent para abrir paquete de la aplicacion extertna usada para las camaras
    public void abrirCam(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }


    //Metodo que se creo para gestionar el comportamiento de los botones que se ocultan y muestran
    public void editWhenThereIsAlert()
    {
            final RelativeLayout linearLayout = (RelativeLayout) findViewById(R.id.layGroup);
            camaraF = (FloatingActionButton) findViewById(R.id.camara2);
            alertButton = (FloatingActionButton) findViewById(R.id.alertaV);

            //Mostrar Layout de chat
            linearLayout.setVisibility(View.VISIBLE);
            //Ocultar botones de akerta
            alertButton.setVisibility(View.INVISIBLE);
            camaraF.setVisibility(View.INVISIBLE);
            //Se muestra la toolbar del chat
            getSupportActionBar().show();
    }

    //Metodo que ejecuta acciones al crearse esta actividad
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_chat);
        Intent intentData = getIntent();
        //Se obtiene informacion de las personas
        idFriend = intentData.getCharSequenceArrayListExtra(StaticConfig.INTENT_KEY_CHAT_ID);
        roomId = intentData.getStringExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID);
        String nameFriend = intentData.getStringExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND);
        //Asignacion de elementos del XML a objetos previamente creados en esta clase
        consersation = new Consersation();
        btnSend = (ImageButton) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(this);
        //Se oculta la barra de acciones
        getSupportActionBar().hide();

        final RelativeLayout linearLayout = (RelativeLayout) findViewById(R.id.layGroup);
        //ocultar el layout de alerta  y habilita el boton camara
        camButton= (MenuItem) findViewById(R.id.camara) ;
        finAlertButton=(MenuItem)findViewById(R.id.finAlerta);
        camaraF =(FloatingActionButton)findViewById(R.id.camara2);
        alertButton=(FloatingActionButton)findViewById(R.id.alertaV);
        //Se agrega el OnClick al boton de alerta
        alertButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            //Se agrega un click largo pues con un click corto se presta a errore y/o mal entendidos
            public boolean onLongClick(View view) {
                //Se llama al metodo de gestion de botones
                editWhenThereIsAlert();
                //LLamamos al metodo que gestiona la notificacion
                notificar(this);
                //Mensaje de informacion
                Toast.makeText(getApplicationContext(), "Alerta enviada \n Cámara y chat habilitados", Toast.LENGTH_LONG).show();
                return true;
            }
        });
        //Se agrega el OnClick al boton de camara
        camaraF.setOnClickListener(new View.OnClickListener() {
            @Override
            //Al HACER CLICK inicia el metodo para abrir el paquete externo de la camara
            public void onClick(View view) {
                abrirCam(ChatActivity.this,"com.xiaomi.smarthome");//Este es el paquete de la app externa
            }
        });


        String base64AvataUser = SharedPreferenceHelper.getInstance(this).getUserInfo().avata;
        if (!base64AvataUser.equals(StaticConfig.STR_DEFAULT_BASE64)) {
            byte[] decodedString = Base64.decode(base64AvataUser, Base64.DEFAULT);
            bitmapAvataUser = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        } else {
            bitmapAvataUser = null;
        }

        //El edit Text del mensaje que se escribe
        editWriteMessage = (EditText) findViewById(R.id.editWriteMessage);
        //Cuando el chat tiene usuarios(No es vacio, osea que existe)
        if (idFriend != null && nameFriend != null) {
            //Ponerle el nombre del amigo como titulo
            getSupportActionBar().setTitle(nameFriend);
            linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            //Se crea el recyclerView del chat que es para mostrar una lista de todos los mensajes... Es la vista de chat
            recyclerChat = (RecyclerView) findViewById(R.id.recyclerChat);
            recyclerChat.setLayoutManager(linearLayoutManager);
            adapter = new ListMessageAdapter(this, consersation, bitmapAvataFriend, bitmapAvataUser);
            //Se extraen los mensajes que ya existen en el chat de FireBase
            FirebaseDatabase.getInstance().getReference().child("message/" + roomId).addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    if (dataSnapshot.getValue() != null) {
                        HashMap mapMessage = (HashMap) dataSnapshot.getValue();
                        Message newMessage = new Message();
                        newMessage.idSender = (String) mapMessage.get("idSender");
                        newMessage.idReceiver = (String) mapMessage.get("idReceiver");
                        newMessage.text = (String) mapMessage.get("text");
                        newMessage.timestamp = (long) mapMessage.get("timestamp");
                        consersation.getListMessageData().add(newMessage);
                        adapter.notifyDataSetChanged();
                        linearLayoutManager.scrollToPosition(consersation.getListMessageData().size() - 1);
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            recyclerChat.setAdapter(adapter);
        }
    }

    @Override
    //Metodo que gestiona los clicks de la toolbar
    public boolean onOptionsItemSelected(MenuItem item) {
        //asignacion de los items de menuopciones.xml a objetos creados previamente
        final RelativeLayout linearLayout = (RelativeLayout) findViewById(R.id.layGroup);
        camaraF = (FloatingActionButton)findViewById(R.id.camara2);
        alertButton=(FloatingActionButton)findViewById(R.id.alertaV);
        //Si se pulso el boton regresar de la tooolbar
        if(item.getItemId() == android.R.id.home){
            //Muestra una Toast y regresa a la actividad anterior matando esta
            Intent result = new Intent();
            result.putExtra("idFriend", idFriend.get(0));
            setResult(RESULT_OK, result);
            Toast.makeText(getApplicationContext(), "Alerta finalizada \n Cámara y chat deshabilitados", Toast.LENGTH_LONG).show();
            this.finish();
        }
        //Cuando se pulsa el item fin alerta
        if (item.getItemId()==R.id.finAlerta)
        {
            //Muetra nuevamente el layout alerta con sus botones y ocuta el chat
            linearLayout.setVisibility(View.INVISIBLE);
            alertButton.setVisibility(View.VISIBLE);
            camaraF.setVisibility(View.VISIBLE);
            //Oculta la toolbar
            getSupportActionBar().hide();
            Toast.makeText(getApplicationContext(), "Alerta finalizada \n Cámara y chat deshabilitados", Toast.LENGTH_LONG).show();
        }
        //Si pulsa el boton de la camara abre el paquete externo de la app
        if (item.getItemId() == R.id.camara)
        {
            abrirCam(ChatActivity.this,"com.xiaomi.smarthome");
        }

        return true;
    }


    //Al puslar el boton fisico de regresar del telefono
    @Override
    public void onBackPressed() {
        Intent result = new Intent();
        //Muestra una Toast y regresa a la actividad anterior matando esta
        result.putExtra("idFriend", idFriend.get(0));
        setResult(RESULT_OK, result);
        Toast.makeText(getApplicationContext(), "Alerta finalizada \n Cámara y chat deshabilitados", Toast.LENGTH_LONG).show();
        this.finish();
    }

    //Metodo que lanza las notificaciones
    public void notificar (View.OnLongClickListener view)
    {
        //El contenido de la notificacion, cuando el usuario acepte la notificacion mostrara el mensaje en le chtat
        String content = "Entrando a Alerta, estoy al tanto";
        if (content.length() > 0) {
            //Se obtienen los datos de quien enviio la alerta y de quienes la reciben
            Message newMessage = new Message();
            newMessage.text = content;
            newMessage.idSender = StaticConfig.UID;
            newMessage.idReceiver = roomId;
            newMessage.timestamp = System.currentTimeMillis();
            FirebaseDatabase.getInstance().getReference().child("message/" + roomId).push().setValue(newMessage);

        }
    }
    //Onclick, en este caso solo se metio el boton de enviar mensaje
    @Override
    public void onClick(View view) {

        //Si se pulso el boton de enviar
        if (view.getId() == R.id.btnSend) {
            //Se obtiene el mensaje del edit text y se agrega a una variable que sera enviada.
            String content = editWriteMessage.getText().toString().trim();
            if (content.length() > 0) {
                editWriteMessage.setText("");
                Message newMessage = new Message();
                newMessage.text = content;
                newMessage.idSender = StaticConfig.UID;
                newMessage.idReceiver = roomId;
                newMessage.timestamp = System.currentTimeMillis();
                //Se agrega el mensaje a la base de datos de Firebase
                FirebaseDatabase.getInstance().getReference().child("message/" + roomId).push().setValue(newMessage);
            }
        }

    }
    //Metodo para agregar los elementos a la toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Con el inflater agregamos los items encontrados en en menuoopciones.xml
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menuopciones,menu);
        return super.onCreateOptionsMenu(menu);
    }
}




class ListMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private Consersation consersation;
    private HashMap<String, Bitmap> bitmapAvata;
    private HashMap<String, DatabaseReference> bitmapAvataDB;
    private Bitmap bitmapAvataUser;

    public ListMessageAdapter(Context context, Consersation consersation, HashMap<String, Bitmap> bitmapAvata, Bitmap bitmapAvataUser) {
        this.context = context;
        this.consersation = consersation;
        this.bitmapAvata = bitmapAvata;
        this.bitmapAvataUser = bitmapAvataUser;
        bitmapAvataDB = new HashMap<>();
    }

    //Para crear la vista del chat que el createViewHolder es como un ListView pero mas grande
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Obtiene los mensajes de los amigos
        if (viewType == ChatActivity.VIEW_TYPE_FRIEND_MESSAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_friend, parent, false);
            return new ItemMessageFriendHolder(view);
            //Obtiene los mensajes del usuario
        } else if (viewType == ChatActivity.VIEW_TYPE_USER_MESSAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_user, parent, false);
            return new ItemMessageUserHolder(view);
        }
        return null;
    }

    //Muestra el viewHolder
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemMessageFriendHolder) {
            ((ItemMessageFriendHolder) holder).txtContent.setText(consersation.getListMessageData().get(position).text);
            Bitmap currentAvata = bitmapAvata.get(consersation.getListMessageData().get(position).idSender);
            if (currentAvata != null) {
                ((ItemMessageFriendHolder) holder).avata.setImageBitmap(currentAvata);
            } else {
                final String id = consersation.getListMessageData().get(position).idSender;
                if(bitmapAvataDB.get(id) == null){
                    bitmapAvataDB.put(id, FirebaseDatabase.getInstance().getReference().child("user/" + id + "/avata"));
                    bitmapAvataDB.get(id).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                String avataStr = (String) dataSnapshot.getValue();
                                if(!avataStr.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                                    byte[] decodedString = Base64.decode(avataStr, Base64.DEFAULT);
                                    ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                                }else{
                                    ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeResource(context.getResources(), R.drawable.default_avata));
                                }
                                notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }
        } else if (holder instanceof ItemMessageUserHolder) {
            ((ItemMessageUserHolder) holder).txtContent.setText(consersation.getListMessageData().get(position).text);
            if (bitmapAvataUser != null) {
                ((ItemMessageUserHolder) holder).avata.setImageBitmap(bitmapAvataUser);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return consersation.getListMessageData().get(position).idSender.equals(StaticConfig.UID) ? ChatActivity.VIEW_TYPE_USER_MESSAGE : ChatActivity.VIEW_TYPE_FRIEND_MESSAGE;
    }

    @Override
    public int getItemCount() {
        return consersation.getListMessageData().size();
    }
}

class ItemMessageUserHolder extends RecyclerView.ViewHolder {
    public TextView txtContent;
    public CircleImageView avata;

    public ItemMessageUserHolder(View itemView) {
        super(itemView);
        txtContent = (TextView) itemView.findViewById(R.id.textContentUser);
        avata = (CircleImageView) itemView.findViewById(R.id.imageView2);
    }
}

class ItemMessageFriendHolder extends RecyclerView.ViewHolder {
    public TextView txtContent;
    public CircleImageView avata;

    public ItemMessageFriendHolder(View itemView) {
        super(itemView);
        txtContent = (TextView) itemView.findViewById(R.id.textContentFriend);
        avata = (CircleImageView) itemView.findViewById(R.id.imageView3);
    }
}
