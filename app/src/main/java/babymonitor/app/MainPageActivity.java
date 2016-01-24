package babymonitor.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * Created by gimmy on 1/16/16.
 */
public class MainPageActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_page);

        final Button parrent_button = (Button) findViewById(R.id.parent_button);
        final Button child_button = (Button) findViewById(R.id.child_button);
        parrent_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent myIntent = new Intent(MainPageActivity.this, ParentActivity.class);
                MainPageActivity.this.startActivity(myIntent);
            }
        });

        child_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent myIntent = new Intent(MainPageActivity.this, ChildActivity.class);
                MainPageActivity.this.startActivity(myIntent);
            }
        });
    }
}
