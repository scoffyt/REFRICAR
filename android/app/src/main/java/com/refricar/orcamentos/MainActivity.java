package com.refricar.orcamentos;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * REFRICAR — Gestão de Orçamentos.
 * O aplicativo abre o site publicado no GitHub Pages; qualquer atualização enviada
 * ao GitHub aparece aqui automaticamente, sem reinstalar o APK.
 */
public class MainActivity extends Activity {

    static final String URL_APP = "https://scoffyt.github.io/REFRICAR/";
    static final String HOST_APP = "scoffyt.github.io";
    static final int REQ_ARQUIVO = 1001;

    private WebView web;
    private LinearLayout telaErro;
    private ValueCallback<Uri[]> retornoArquivo;
    private boolean carregou = false;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);

        FrameLayout raiz = new FrameLayout(this);
        web = new WebView(this);
        web.setBackgroundColor(Color.TRANSPARENT);
        raiz.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        telaErro = criarTelaErro();
        telaErro.setVisibility(View.GONE);
        raiz.addView(telaErro, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(raiz);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUserAgentString(s.getUserAgentString() + " RefricarApp/1");
        CookieManager.getInstance().setAcceptCookie(true);

        web.addJavascriptInterface(new Ponte(), "RefricarAndroid");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return abrirFora(r.getUrl());
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                if (!carregou) {
                    carregou = true;
                    getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#F3F6FB")));
                }
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) mostrarErro();
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (retornoArquivo != null) retornoArquivo.onReceiveValue(null);
                retornoArquivo = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                String tipo = "*/*";
                String[] aceitos = p.getAcceptTypes();
                if (aceitos != null) {
                    for (String a : aceitos) {
                        if (a != null && a.startsWith("image/")) { tipo = "image/*"; break; }
                    }
                }
                i.setType(tipo);
                try {
                    startActivityForResult(Intent.createChooser(i, "Escolher arquivo"), REQ_ARQUIVO);
                } catch (ActivityNotFoundException ex) {
                    retornoArquivo = null;
                    return false;
                }
                return true;
            }
        });

        if (estado != null) web.restoreState(estado);
        else web.loadUrl(URL_APP);
    }

    private boolean abrirFora(Uri u) {
        String esquema = u.getScheme() == null ? "" : u.getScheme();
        if ((esquema.equals("https") || esquema.equals("http")) && HOST_APP.equalsIgnoreCase(u.getHost())) {
            return false;
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, u);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (ActivityNotFoundException ex) {
            aviso("Nenhum aplicativo para abrir este link.");
        }
        return true;
    }

    private LinearLayout criarTelaErro() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setBackgroundColor(Color.parseColor("#0A1A31"));
        int p = (int) (32 * getResources().getDisplayMetrics().density);
        l.setPadding(p, p, p, p);
        TextView t = new TextView(this);
        t.setText("Sem conexão com a internet");
        t.setTextColor(Color.WHITE);
        t.setTextSize(20);
        t.setGravity(Gravity.CENTER);
        TextView d = new TextView(this);
        d.setText("O REFRICAR precisa de internet para acessar seus orçamentos. Verifique a conexão e tente novamente.");
        d.setTextColor(Color.parseColor("#B9C9E2"));
        d.setTextSize(15);
        d.setGravity(Gravity.CENTER);
        d.setPadding(0, p / 2, 0, p);
        Button b = new Button(this);
        b.setText("Tentar novamente");
        b.setOnClickListener(v -> {
            telaErro.setVisibility(View.GONE);
            web.reload();
        });
        l.addView(t);
        l.addView(d);
        l.addView(b);
        return l;
    }

    private void mostrarErro() {
        runOnUiThread(() -> telaErro.setVisibility(View.VISIBLE));
    }

    @Override
    public void onBackPressed() {
        if (telaErro.getVisibility() == View.VISIBLE) { finish(); return; }
        web.evaluateJavascript("(window.refricarVoltar && window.refricarVoltar()) ? '1' : '0'", valor -> {
            if (!"\"1\"".equals(valor)) {
                if (web.canGoBack()) web.goBack();
                else finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int req, int res, Intent dados) {
        super.onActivityResult(req, res, dados);
        if (req == REQ_ARQUIVO && retornoArquivo != null) {
            Uri[] r = null;
            if (res == RESULT_OK && dados != null && dados.getData() != null) r = new Uri[]{dados.getData()};
            retornoArquivo.onReceiveValue(r);
            retornoArquivo = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (telaErro.getVisibility() == View.VISIBLE && temInternet()) {
            telaErro.setVisibility(View.GONE);
            web.reload();
        }
    }

    private boolean temInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return true;
        NetworkInfo n = cm.getActiveNetworkInfo();
        return n != null && n.isConnected();
    }

    private void aviso(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }

    private File arquivoTemporario(String nome, String b64) throws Exception {
        File dir = new File(getCacheDir(), "compartilhados");
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, nomeSeguro(nome));
        try (FileOutputStream os = new FileOutputStream(f)) {
            os.write(Base64.decode(b64, Base64.DEFAULT));
        }
        return f;
    }

    private Uri uriDe(File f) {
        return FileProvider.getUriForFile(this, getPackageName() + ".arquivos", f);
    }

    private static String nomeSeguro(String nome) {
        String n = nome == null ? "arquivo" : nome.replaceAll("[\\\\/:*?\"<>|]", "-");
        return n.isEmpty() ? "arquivo" : n;
    }

    /** Funções chamadas pelo site (window.RefricarAndroid). */
    class Ponte {

        @JavascriptInterface
        public String versao() {
            return "1";
        }

        @JavascriptInterface
        public String salvar(String nome, String mime, String b64) {
            try {
                byte[] dados = Base64.decode(b64, Base64.DEFAULT);
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.MediaColumns.DISPLAY_NAME, nomeSeguro(nome));
                    cv.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                    cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/REFRICAR");
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (uri == null) throw new Exception("sem destino");
                    try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                        if (os == null) throw new Exception("sem destino");
                        os.write(dados);
                    }
                    aviso("Salvo em Downloads/REFRICAR: " + nome);
                } else {
                    File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    if (dir == null) dir = new File(getFilesDir(), "Download");
                    if (!dir.exists()) dir.mkdirs();
                    File f = new File(dir, nomeSeguro(nome));
                    try (FileOutputStream os = new FileOutputStream(f)) { os.write(dados); }
                    abrirArquivo(f, mime);
                }
                return "ok";
            } catch (Exception e) {
                aviso("Não foi possível salvar o arquivo.");
                return "erro";
            }
        }

        @JavascriptInterface
        public String abrir(String nome, String mime, String b64) {
            try {
                abrirArquivo(arquivoTemporario(nome, b64), mime);
                return "ok";
            } catch (Exception e) {
                aviso("Não foi possível abrir o arquivo.");
                return "erro";
            }
        }

        @JavascriptInterface
        public String compartilhar(String nome, String mime, String b64, String texto) {
            try {
                Uri uri = uriDe(arquivoTemporario(nome, b64));
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType(mime);
                i.putExtra(Intent.EXTRA_STREAM, uri);
                if (texto != null && !texto.isEmpty()) i.putExtra(Intent.EXTRA_TEXT, texto);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Intent c = Intent.createChooser(i, "Compartilhar orçamento");
                c.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(c);
                return "ok";
            } catch (Exception e) {
                aviso("Não foi possível compartilhar o arquivo.");
                return "erro";
            }
        }

        @JavascriptInterface
        public String whatsapp(String nome, String mime, String b64, String texto, String fone) {
            try {
                Uri uri = uriDe(arquivoTemporario(nome, b64));
                String[] pacotes = {"com.whatsapp", "com.whatsapp.w4b"};
                for (String pacote : pacotes) {
                    if (getPackageManager().getLaunchIntentForPackage(pacote) == null) continue;
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setPackage(pacote);
                    i.setType(mime);
                    i.putExtra(Intent.EXTRA_STREAM, uri);
                    if (texto != null && !texto.isEmpty()) i.putExtra(Intent.EXTRA_TEXT, texto);
                    if (fone != null && !fone.isEmpty()) i.putExtra("jid", fone + "@s.whatsapp.net");
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(i);
                        return "ok";
                    } catch (ActivityNotFoundException ignored) {
                    }
                }
                return compartilhar(nome, mime, b64, texto).equals("ok") ? "sem-whatsapp" : "erro";
            } catch (Exception e) {
                aviso("Não foi possível abrir o WhatsApp.");
                return "erro";
            }
        }
    }

    private void abrirArquivo(File f, String mime) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uriDe(f), mime);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            aviso("Instale um leitor de PDF para abrir o arquivo.");
        }
    }
}
