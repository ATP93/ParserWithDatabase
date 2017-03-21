package com.denisgalajda.parserwithdatabase;

import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private static final String SAVED_SQL_QUERIES = "sql_queries";
    private static final String TEST_DB_NAME = "database.db";

    private Button mRunSqlButton;
    private Handler mHandler = new Handler();
    private ProgressBar mProgressBar;
    private int mProgressStatus = 0;
    private SQLiteDatabase mTestDatabase;

    private ArrayList<String> parsedSqlQueriesArrayList;

    /*
     * Nieco ako main - bezi hned ako spustis appku
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // priradenie layout xml suboru k Aktivite
        setContentView(R.layout.activity_main);

        // suvisi s zivotnym cyklom Activity
        if (savedInstanceState != null) {
            parsedSqlQueriesArrayList = savedInstanceState.getStringArrayList(SAVED_SQL_QUERIES);
        } else {
            try {
                SqlXmlParser sqlXmlParser = new SqlXmlParser();
                InputStream inputStream = getApplicationContext().getAssets().open("sql.xml");
                parsedSqlQueriesArrayList = sqlXmlParser.parse(inputStream);

            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }
        }

        // "najdenie" komponentov z layoutu
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mRunSqlButton = (Button) findViewById(R.id.button_run_sql);
        mRunSqlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TestSqlAsyncTask().execute();
            }
        });

    }

    // suvisi s zivotnym cyklom Activity
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(SAVED_SQL_QUERIES, parsedSqlQueriesArrayList);
    }


    /*
     * Asynchronne vlakno kde je samotne vykonanie SQL prikazov - aby neblokovali UI thread
     */
    private class TestSqlAsyncTask extends AsyncTask<Void, Void, Long> {

        @Override
        protected void onPreExecute() {
            mProgressStatus = 0;
            mProgressBar.setMax(parsedSqlQueriesArrayList.size());
            mProgressBar.setProgress(mProgressStatus);
            mProgressBar.setVisibility(View.VISIBLE);
            mRunSqlButton.setEnabled(false);
            mTestDatabase = openOrCreateDatabase("TEST_DB_NAME",MODE_PRIVATE,null);
        }

        @Override
        protected Long doInBackground(Void... params) {

            long tStart = System.currentTimeMillis();

            for (String sqlQuery:parsedSqlQueriesArrayList) {
                try {
                    mTestDatabase.execSQL(sqlQuery);
                    /* TODO rozlisit SELECT
                        - ako mas v definicii metody execSQL():
                        - "Execute a single SQL statement that is NOT a SELECT "
                        - "or any other SQL statement that returns data"
                        - na selecty pouzi mTestDatabase.rawQuery(sqlQuery, null);
                        - potrebujes teda rozlisit kedy to je select a kedy nie
                        - napr. cez atribut v XML alebo si to vyparsujes
                     */

                } catch (Exception e) {
                    e.printStackTrace();
                }

                // "naplnovanie" progress baru
                mProgressStatus += 1;
                mHandler.post(new Runnable() {
                    public void run() {
                        mProgressBar.setProgress(mProgressStatus);
                    }
                });
            }

            long tEnd = System.currentTimeMillis();

            return tEnd - tStart;
        }

        @Override
        protected void onPostExecute(Long elapsedMiliseconds) {
            Toast.makeText(getApplicationContext(), "SQL queries executed in " + elapsedMiliseconds + " ms.", Toast.LENGTH_SHORT).show();

            /* TODO precistit DB
                - ak chces test spustit opakovane potrebujes si vycistit DB (existujuce tabulky...)
                - bud si tabulky podrobujes priamo v xmlku ktore budes spustat
                - alebo si to podropujes manualne cez mTestDatabase.delete(table_name, null, null);
                - existuje metoda getAplicationContext.deleteDatabase(TEST_DB_NAME), kt. precisti db
                - aby fungovala ale musis mat root povolenia (rootnuty telefon)
                - je to tak chujovo lebo normalne sa nezvyknu dynamicky vytvarat a dropovat tabulky
                - namiesto toho sa s db pracuje s SQL Helperom
             */

            if (mTestDatabase.isOpen()) {
                mTestDatabase.close();
            }

            mProgressBar.setVisibility(View.INVISIBLE);
            mRunSqlButton.setEnabled(true);

        }
    }
}
