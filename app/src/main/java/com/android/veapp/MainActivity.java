package com.android.veapp;

//Importaciones necesarias en el proyecto
import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.android.veapp.data.StaticConfig;
import com.android.veapp.service.ServiceUtils;
import com.android.veapp.ui.FriendsFragment;
import com.android.veapp.ui.GroupFragment;
import com.android.veapp.ui.LoginActivity;
import com.android.veapp.ui.UserProfileFragment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";
    private ViewPager viewPager;
    private TabLayout tabLayout = null;
    public static String STR_FRIEND_FRAGMENT = "FRIEND";
    public static String STR_GROUP_FRAGMENT = "GROUP";
    public static String STR_INFO_FRAGMENT = "INFO";

    private FloatingActionButton floatButton;
    private ViewPagerAdapter adapter;


    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser user;





    //
    //Metodo que ejecuta acciones al ingresar a esta actividad
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // asignacion del XML al objeto creado arriba

        //Validacion para ver la version de android del telefono y saber si debe pedir permiso
        //Para la lectura de los archivos para poder mostrar los PDF's de ayuda
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            try{
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            //llama al metodo para Verificar permisos para Android 6.0+
            checkExternalStoragePermission();
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        //Validacion para agregar los elementos de la toolbar de la aplicacion pricpial
        //Y asigna el titulo que tendra
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if(toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle("VEAPP");
        }

        //ASignacion de boton flotante y pestañas de la app
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        floatButton = (FloatingActionButton) findViewById(R.id.fab);

        initTab();
        initFirebase();
    }

    //Metodo para iniciar la base de datos que es Firebase
    //Base de datos de google
    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    StaticConfig.UID = user.getUid();
                } else {
                    MainActivity.this.finish();
                    // Si es usuario esta loggeado se dirige a la vista principal de la app
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };
    }


    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
        ServiceUtils.stopServiceFriendChat(getApplicationContext(), false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onDestroy() {
        ServiceUtils.startServiceFriendChat(getApplicationContext());
        super.onDestroy();
    }


    //Metodo usado para asignar color al TabLayout(Pestañas de la app) dependiendo de donde se encuentre el
    //Usuario y llama al metodo de mostrar iconos
    private void initTab() {
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setSelectedTabIndicatorColor(getResources().getColor(R.color.colorIndivateTab));
        setupViewPager(viewPager);
        tabLayout.setupWithViewPager(viewPager);
        setupTabIcons();
    }

    //Metodo para asignar los iconos a las pestañas
    private void setupTabIcons() {
        int[] tabIcons = {
                R.drawable.ic_tab_person,
                R.drawable.ic_tab_group,
                R.drawable.ic_tab_infor
        };

        tabLayout.getTabAt(0).setIcon(tabIcons[0]);
        tabLayout.getTabAt(1).setIcon(tabIcons[1]);
        tabLayout.getTabAt(2).setIcon(tabIcons[2]);
    }

    //Metodo que sirve para saber en que pestaña se encuentra el usuario
    private void setupViewPager(ViewPager viewPager) {

        //Al adapter se agrega las pestañas existentes
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(new FriendsFragment(), STR_FRIEND_FRAGMENT);
        adapter.addFrag(new GroupFragment(), STR_GROUP_FRAGMENT);
        adapter.addFrag(new UserProfileFragment(), STR_INFO_FRAGMENT);
        floatButton.setOnClickListener(((FriendsFragment) adapter.getItem(0)).onClickFloatButton.getInstance(this));
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);
        //Se agrega el listener del View pager y dentro sus metodos
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }
            //metodo del ViewPager para saber que pestaña esta seleccionada
            @Override
            public void onPageSelected(int position) {


                ServiceUtils.stopServiceFriendChat(MainActivity.this.getApplicationContext(), false);
                /* Si el usuario se encuentra en la pestaña de sus amigos
                    El boton flotante de agregar amigo se pone visible
                    Al boton flotante se asigna  su imagen de agregar amigo
                    y la accion de agregar amigo
                 */
                if (adapter.getItem(position) instanceof FriendsFragment) {
                    floatButton.setVisibility(View.VISIBLE);
                    floatButton.setOnClickListener(((FriendsFragment) adapter.getItem(position)).onClickFloatButton.getInstance(MainActivity.this));
                    floatButton.setImageResource(R.drawable.plus);

                    /*
                        Si el usuario se encuentra en la pestaña de sus grupos
                        El boton se cambia al icono de agregar grupo
                        El boton se cambia la accion a agregar grupo

                     */
                } else if (adapter.getItem(position) instanceof GroupFragment) {
                    floatButton.setVisibility(View.VISIBLE);
                    floatButton.setOnClickListener(((GroupFragment) adapter.getItem(position)).onClickFloatButton.getInstance(MainActivity.this));
                    floatButton.setImageResource(R.drawable.ic_float_add_group);
                    /*
                        Si no cumple con las anteriores quiere decir que esta en la pestaña de configuracion
                        Entnces el boton se quita
                     */
                } else {
                    floatButton.setVisibility(View.GONE);

                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    //Metodo que crea el menu de la toolbar y en su linea que contiene

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Agrega los itmen(opciones) definidos en menu_main.xml
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //En este metodo se manejan los clicks que se hacen en cada itrem del toolbar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        //Al presionar el about, muestra un toast con información de la App
        if (id == R.id.about) {
            Toast.makeText(this, "VEAPP entrega final", Toast.LENGTH_LONG).show();
            return true;
        }
        //Ap presionar en ayuda, mmuestra un PDF que tiene un manual de usuario
        if (item.getItemId()==R.id.verPDF) {
            //SE manda llamar a un metodo que copia el archivo que esta dentro de alrchivo a la sd
            //del telefono
            CopyRawToSDCard(R.raw.ayuda, Environment.getExternalStorageDirectory() + "/ayuda.pdf" );
            File pdfFile = new File(Environment.getExternalStorageDirectory(),"/ayuda.pdf" );
            if (pdfFile.exists()){ //Revisa si el archivo existe!
                Uri path = Uri.fromFile(pdfFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                //define el tipo de archivo
                intent.setDataAndType(path, "application/pdf");
                intent.setFlags(Intent. FLAG_ACTIVITY_CLEAR_TOP);
                //Inicia pdf viewer
                startActivity(intent);
            } else {
                //Si no existe el archivo da este error
                Toast.makeText(getApplicationContext(), "No existe archivo! ", Toast.LENGTH_SHORT).show();
            }
        }
        //Al pulsar en ver privacidad muesta un pdf con iformación de la privacidad
        if (item.getItemId()==R.id.verPrivacidad){
            CopyRawToSDCard(R.raw.privacidad, Environment.getExternalStorageDirectory() + "/privacidad.pdf" );
            File pdfFile = new File(Environment.getExternalStorageDirectory(),"/privacidad.pdf" );//File path
            if (pdfFile.exists()){ //Revisa si el archivo existe!
                Uri path = Uri.fromFile(pdfFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                //define el tipo de archivo
                intent.setDataAndType(path, "application/pdf");
                intent.setFlags(Intent. FLAG_ACTIVITY_CLEAR_TOP);
                //Inicia pdf viewer
                startActivity(intent);
            } else {
                Toast.makeText(getApplicationContext(), "No existe archivo! ", Toast.LENGTH_SHORT).show();
            }

            //Al pulsar en ver terminos muesta un pdf con iformación de los terminos de usuario
        }
        if (item.getItemId()==R.id.verTerminos){
            CopyRawToSDCard(R.raw.terminos, Environment.getExternalStorageDirectory() + "/terminos.pdf" );
            File pdfFile = new File(Environment.getExternalStorageDirectory(),"/terminos.pdf" );//File path
            if (pdfFile.exists()){ //Revisa si el archivo existe!
                Uri path = Uri.fromFile(pdfFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                //define el tipo de archivo
                intent.setDataAndType(path, "application/pdf");
                intent.setFlags(Intent. FLAG_ACTIVITY_CLEAR_TOP);
                //Inicia pdf viewer
                startActivity(intent);
            } else {
                Toast.makeText(getApplicationContext(), "No existe archivo! ", Toast.LENGTH_SHORT).show();
            }

        }
        return super.onOptionsItemSelected(item);
    }


    //Metodo para verificar los permisos del telefono
    private void checkExternalStoragePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Log.i("Mensaje", "No se tiene permiso para leer.");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 225);
        } else {
            Log.i("Mensaje", "Se tiene permiso para leer!");
        }
    }


    //Meotod que copia el pdf que esta dentro del paquete a la Sd del telefono para poder vizualizar PDF's
    public void CopyRawToSDCard ( int id, String path){

        InputStream in = getResources().openRawResource(id);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            byte[] buff = new byte[1024];
            int read = 0;
            while ((read = in.read(buff)) > 0) {
                out.write(buff, 0, read);
            }
            in.close();
            out.close();
            Log.i(TAG, "copyFile, success!");
        } catch (FileNotFoundException e) {
            Log.e(TAG, "copyFile FileNotFoundException " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "copyFile IOException " + e.getMessage());
        }
    }


    //Clase para ver las pestañas
    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {


            return null;
        }
    }
}