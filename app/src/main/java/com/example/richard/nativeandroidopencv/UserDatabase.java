package com.example.richard.nativeandroidopencv;

import java.util.Vector;

/**
 * Created by Richard on 2017/10/13.
 */

public class UserDatabase
{
	public Vector<Vector<Integer>> irisCodes;
	public Vector<String> names;

	public UserDatabase(Vector<Vector<Integer>> irisCodesInput, Vector<String> namesInput)
	{
		irisCodes = irisCodesInput;
		names = namesInput;
	}

	public void addUser(Vector<Integer> newIrisCode, String newName)
	{
		irisCodes.add(newIrisCode);
		names.add(newName);
	}

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
