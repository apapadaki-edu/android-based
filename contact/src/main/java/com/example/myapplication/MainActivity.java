package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity{
    private LinearLayout parentLinearLayout;
    private EditText contactField;
    private EditText bodyField;
    private EditText subjectField;
    /* OWASP regex a more strict version of rfc 5322*/
    private static final String EMAIL_VALIDATION_PATTERN = "^\\w+([\\.-]?\\w+)*@\\w+([\\.-]?\\w+)*(\\.\\w{2,3})+$";
    /* simple custom regex, allowed patterns: +cc 10digits, +cc10digits, 10digits*/
    private static final String PHONE_VALIDATION_PATTERN = "(\\+\\d{2})? ?\\d{10}";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        //get references for edit text fields and the parent layout
        subjectField = (EditText) findViewById(R.id.subject_edit_text);
        contactField = (EditText)findViewById(R.id.contact_edit_text);
        bodyField = (EditText) findViewById(R.id.body_edit_text);
        parentLinearLayout=(LinearLayout) findViewById(R.id.parent_linear_layout);


        Button sendEmailButton = (Button) findViewById(R.id.button_email);
        Button sendSMSButton = (Button) findViewById(R.id.button_sms);
        Button clearBodyButton = (Button) findViewById(R.id.button_clear);

        //add listeners, first two are defined later
        sendEmailButton.setOnClickListener(this::sendEmail);
        sendSMSButton.setOnClickListener(this::sendSMS);
        clearBodyButton.setOnClickListener(view -> bodyField.getText().clear());


        //listener that keeps track of user input in the contact field and if a valid email
        //address is given it adds a linear layout view (email_subject_field.xml) with an email
        //subject text field
        contactField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //TODO Auto-generated method stub
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //reference to subject linear view
                View subjectField = (View) findViewById(R.id.subject_field_container);

                //if subject view not in parent layout and valid email given add the subject view
                if (subjectField == null && editable.toString().length() > 0
                    && editable.toString().matches(EMAIL_VALIDATION_PATTERN))
                {
                    addSubjectField();
                }

                //if the email address field empties remove the subject view
                if (editable.toString().length() == 0 && subjectField != null)
                {
                    parentLinearLayout.removeView(subjectField);
                }
            }
        });

    }

    //send sms listener
    public void sendSMS(View view){
        String contact = contactField.getText().toString();

        //if a valid number is given and if so creates the appropriate intent and opens the message app
        if (contact.trim().length() > 0 && contact.matches(PHONE_VALIDATION_PATTERN))
        {
            Intent sendSMS = new Intent(Intent.ACTION_SENDTO);
            sendSMS.setData(Uri.parse("smsto:" + contact));
            sendSMS.putExtra("sms_body", bodyField.getText().toString());
            startActivity(sendSMS);
        }
        else
        {
            //if not valid phone number throw toast message
            Toast.makeText(getApplicationContext(), getString(R.string.invalid_phone_number_toast), Toast.LENGTH_SHORT).show();
        }

    }

    //send email listener, works in a similar manner to the sms listener
    public void sendEmail(View view){
        String address = contactField.getText().toString();

        if(address.trim().length() > 0 && address.matches(EMAIL_VALIDATION_PATTERN))
        {
            Intent sendEmail = new Intent(Intent.ACTION_SEND);
            sendEmail.putExtra(Intent.EXTRA_EMAIL, new String[] {address});
            sendEmail.putExtra(Intent.EXTRA_TEXT,  bodyField.getText().toString());

            //if subject editText field present in parent layout retrieve its contents
            if (subjectField != null)
            {
                sendEmail.putExtra(Intent.EXTRA_SUBJECT, subjectField.getText().toString());
            }

            // to open apps that can handle emails
            sendEmail.setType("message/rfc822");

            //create chooser of available apps as specified above
            startActivity(Intent.createChooser(sendEmail, getString(R.string.email_app_chooser_prompt)));

        }
        else
        {
            Toast.makeText(getApplicationContext(), getString(R.string.invalid_email_toast), Toast.LENGTH_SHORT).show();
        }

    }

    //adds the subject linear layout specified by appropriate xml file to parent layout using an inflater
    public void addSubjectField(){
        LayoutInflater inflater = (LayoutInflater) getSystemService((Context.LAYOUT_INFLATER_SERVICE));
        final View subjectView = inflater.inflate(R.layout.email_subject_field, null);
        parentLinearLayout.addView(subjectView, 2);
    }

}
