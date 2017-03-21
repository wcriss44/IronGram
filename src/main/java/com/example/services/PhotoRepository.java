package com.example.services;

import com.example.entities.Photo;
import com.example.entities.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PhotoRepository extends CrudRepository<Photo, Integer> {
    List<Photo> findByRecipient(User recipient);
}