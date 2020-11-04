package com.android.veapp.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


//Machote del chat
public class Room {
    public ArrayList<String> member;
    public Map<String, String> groupInfo;

    public Room(){
        member = new ArrayList<>();
        groupInfo = new HashMap<String, String>();
    }
}
