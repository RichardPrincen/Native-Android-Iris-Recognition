package com.example.richard.nativeandroidopencv;

import java.util.Vector;

public class UserDatabase
{
	public Vector<Vector<Integer>> irisCodes;
	public Vector<String> names;

	//Constructor
	public UserDatabase(Vector<Vector<Integer>> irisCodesInput, Vector<String> namesInput)
	{
		irisCodes = irisCodesInput;
		names = namesInput;
	}

	//Adds a user to the database
	public void addUser(Vector<Integer> newIrisCode, String newName)
	{
		irisCodes.add(newIrisCode);
		names.add(newName);
	}

	//Removes a user from the database
	public void deleteUser(String nameToBeDeleted)
	{
		int i;
		for (i = 0;i < names.size(); i++)
			if (names.elementAt(i) == nameToBeDeleted)
				break;

		names.remove(i);
		irisCodes.remove(i);
	}
}
