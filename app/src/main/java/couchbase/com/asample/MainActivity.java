package couchbase.com.asample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.replicator.Replication;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.support_simple_spinner_dropdown_item);

        // Create a manager
        com.couchbase.lite.Manager manager = null;
        try {
            manager = new Manager(new AndroidContext(getApplicationContext()), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // When is forestdb required
        // com.couchbase.cbforest.Database forestDatabase = null;

        // Create or open the database named testload
        Database database = null;
        try {
            database = manager.getDatabase("testload");
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        // The properties that will be saved on the document
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("title", "Couchbase Mobile");
        properties.put("sdk", "Android");
        // Create a new document
        Document document = database.createDocument();
        // Save the document to the database
        try {
            document.putProperties(properties);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        // Log the generated document ID
        Log.d("Debug1", String.format("Document ID :: %s", document.getId()));
        Log.d("Debug2", String.format("Title %s with %s", (String) document.getProperty("title"), (String) document.getProperty("_id")));

        // Create replicators to push & pull changes to & from Sync Gateway.
        URL url = null;
        try {
            url = new URL("http://10.112.151.101:4984/testload");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Create a view and register its map function:
        final View getID = database.getView("getID");
        getID.setMap(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                emitter.emit(document.get("_id"), null);
            }
        },document.getCurrentRevisionId());
        // Set up a query for a view that indexes blog posts, to get the latest:
        Query query = database.getView("getID").createQuery();
        try {
            QueryEnumerator result = query.run();
            for (Iterator<QueryRow> item = result; item.hasNext(); ) {
                QueryRow row = item.next();
                // Should see the UUID's of all documents in logs
                Log.d("view result...", row.getKey().toString());
            }
        } catch (Exception e) {
            Log.println(Log.ERROR, e.toString(), "ouch");
        }

        Replication push = database.createPushReplication(url);
        Replication pull = database.createPullReplication(url);
        push.setContinuous(true);
        pull.setContinuous(true);

        // Start replicators
        push.start();
        pull.start();

    }
}