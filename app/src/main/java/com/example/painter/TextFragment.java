package com.example.painter;

import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.example.painter.Adapter.ColorAdapter;
import com.example.painter.Interface.TextFragmentListener;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;


public class TextFragment extends BottomSheetDialogFragment implements ColorAdapter.ColorAdapterListener {

    int selectedColor = Color.parseColor("#000000");
    EditText add_text_et;
    RecyclerView recycler_color;
    Button add_text_btn;
    TextFragmentListener listener;


    public void setListener(TextFragmentListener listener){
        this.listener = listener;
    }

    static TextFragment instance;
    public static TextFragment getInstance(){
        if(instance == null)
            instance = new TextFragment();
        return instance;
    }

    public TextFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_text, container, false);

        add_text_et = (EditText) view.findViewById(R.id.add_text_et);
        add_text_btn = (Button)view.findViewById(R.id.add_text_btn);
        recycler_color = (RecyclerView) view.findViewById(R.id.recycler_color);
        recycler_color.setHasFixedSize(true);
        recycler_color.setLayoutManager(new LinearLayoutManager(getActivity(),LinearLayoutManager.HORIZONTAL,false));

        ColorAdapter colorAdapter = new ColorAdapter(getContext(), (ColorAdapter.ColorAdapterListener) this);
        recycler_color.setAdapter(colorAdapter);

        add_text_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onTextButtonClicked(add_text_et.getText().toString(),selectedColor);
            }
        });


        return view;
    }

    @Override
    public void onColorSelected(int color) {
        selectedColor = color;
    }
}