package com.ms.search_spot;

public class Item { // 리스트 정보
    String title;
    String desc;

    String getTitle() {
        return this.title;
    }

    String getDesc() {
        return desc;
    }

    Item(String title, String desc) {
        this.title = title;
        this.desc = desc;
    }
}