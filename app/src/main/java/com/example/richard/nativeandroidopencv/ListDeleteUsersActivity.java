package com.example.richard.nativeandroidopencv;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Vector;


public class ListDeleteUsersActivity extends ListActivity
{
	private UserDatabase userdb;
	private ListView usersListView;

	//These two are used to take the vector of names and populate the listview
	private final ArrayList<String> namesList = new ArrayList<>();
	private ArrayAdapter<String> adapter;

	//Create the activity and initialize the list view
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_list_delete_users);

		loadUserDatabase();
		usersListView = (ListView) findViewById(android.R.id.list);
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, namesList);
		setListAdapter(adapter);

		populateListView();

		//Delete a user on touch of their username
		usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> adapter, View v, int position, long id)
			{
				String item = (String) adapter.getItemAtPosition(position);
				userdb.deleteUser(item);
				saveUserDatabase();
				finish();
			}
		});
	}

	//Load the data from file into the userdb object
	public void loadUserDatabase()
	{
		try
		{
			FileInputStream irisCodesFileInputStream = openFileInput("irisCodes");
			ObjectInputStream irisCodesObjectInputStream = new ObjectInputStream(irisCodesFileInputStream);
			Vector<Vector<Integer>> irisCodes = (Vector<Vector<Integer>>)irisCodesObjectInputStream.readObject();
			irisCodesObjectInputStream.close();

			FileInputStream namesFileInputStream = openFileInput("names");
			ObjectInputStream namesObjectInputStream = new ObjectInputStream(namesFileInputStream);
			Vector<String> names = (Vector<String>)namesObjectInputStream.readObject();
			namesObjectInputStream.close();

			userdb = new UserDatabase(irisCodes, names);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	//Save the data in the userdb object to file
	public void saveUserDatabase()
	{
		try
		{
			FileOutputStream irisCodesFileOutputStream = openFileOutput("irisCodes", Context.MODE_PRIVATE);
			ObjectOutputStream irisCodesObjectOutputStream = new ObjectOutputStream(irisCodesFileOutputStream);
			irisCodesObjectOutputStream.writeObject(userdb.irisCodes);
			irisCodesObjectOutputStream.close();
			irisCodesObjectOutputStream.flush();

			FileOutputStream namesFileOutputStream = openFileOutput("names", Context.MODE_PRIVATE);
			ObjectOutputStream namesObjectOutputStream = new ObjectOutputStream(namesFileOutputStream);
			namesObjectOutputStream.writeObject(userdb.names);
			namesObjectOutputStream.close();
			namesObjectOutputStream.flush();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	//Fill the listview
	public void populateListView()
	{
		for (int i = 0;i < userdb.names.size(); i++)
			namesList.add(userdb.names.elementAt(i));
		adapter.notifyDataSetChanged();
	}

	//Changed to done button. just goes back to main menu
	public void buttonDeleteClicked(View view)
	{
		finish();
	}
}
