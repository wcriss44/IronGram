package com.example.controllers;

import com.example.entities.Photo;
import com.example.entities.User;
import com.example.services.PhotoRepository;
import com.example.services.UserRepository;
import com.example.utilities.PasswordStorage;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@RestController
public class IronGramController {
    Timer timer;
    @Autowired
    UserRepository users;

    @Autowired
    PhotoRepository photos;

    Server dbui = null;

    @PostConstruct
    public void init() throws SQLException {
        dbui = Server.createWebServer().start();
    }

    @PreDestroy
    public void destroy() {
        dbui.stop();
    }
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public User login(String username, String password, HttpSession session, HttpServletResponse response) throws Exception {
        User user = users.findFirstByName(username);
        if (user == null) {
            user = new User(username, PasswordStorage.createHash(password));
            users.save(user);
        }
        else if (!PasswordStorage.verifyPassword(password, user.getPassword())) {
            throw new Exception("Wrong password");
        }
        session.setAttribute("username", username);
        response.sendRedirect("/");
        return user;
    }

    @RequestMapping("/logout")
    public void logout(HttpSession session, HttpServletResponse response) throws IOException {
        session.invalidate();
        response.sendRedirect("/");
    }

    @RequestMapping(path = "/user", method = RequestMethod.GET)
    public User getUser(HttpSession session) {
        String username = (String) session.getAttribute("username");
        return users.findFirstByName(username);
    }
    @RequestMapping("/upload")
    public Photo upload(HttpSession session, HttpServletResponse response, String receiver, MultipartFile photo, int time, boolean status
    ) throws Exception {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            throw new Exception("Not logged in.");
        }

        User senderUser = users.findFirstByName(username);
        User receiverUser = users.findFirstByName(receiver);
        File dir = new File("public/photons");
        dir.mkdirs();

        if (receiverUser == null) {
            throw new Exception("Receiver name doesn't exist.");
        }

        if (!photo.getContentType().startsWith("image")) {
            throw new Exception("Only images are allowed.");
        }

        File photoFile = File.createTempFile("photo", photo.getOriginalFilename(), dir);
        FileOutputStream fos = new FileOutputStream(photoFile);
        fos.write(photo.getBytes());

        Photo p = new Photo();
        p.setSender(senderUser);
        p.setRecipient(receiverUser);
        p.setFilename(photoFile.getName());
        p.setStatus(status);
        p.setSeconds(time);
        photos.save(p);

        response.sendRedirect("/");

        return p;
    }
    @RequestMapping("/photos")
    public List<Photo> showPhotos(HttpSession session) throws Exception {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            throw new Exception("Not logged in.");
        }
        User user = users.findFirstByName(username);
        for (Photo photo: photos.findByRecipient(user)) {
            deleteCaller(photo);
        }
        return photos.findByRecipient(user);
    }
    @RequestMapping("/public-photos/{senderName}")
    public List<Photo> showPublicPhotos(@PathVariable("senderName") String senderName) throws Exception {
        User user = users.findFirstByName(senderName);

        return photos.findBySenderAndStatus(user, false);
    }
    public synchronized void deleteCaller(Photo photo) {
        this.timer = new Timer();

        TimerTask action = new TimerTask() {
            public void run() {
                delete(photo);
            }

        };

        this.timer.schedule(action, photo.getSeconds() * 1000);
    }
    public void delete(Photo photo){
        photos.delete(photo.getId());
        File file = new File("public/photons/" + photo.getFilename());
        file.delete();
    }
}
