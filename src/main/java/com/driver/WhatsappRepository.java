package com.driver;

import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most one group
    //You can use the below mentioned hashmaps or delete these and create your own.
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<Message, User> senderMap;
    private HashMap<Group, User> adminMap;
    private HashSet<String> userMobile;
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository(){
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userMobile = new HashSet<>();
        this.customGroupCount = 0;
        this.messageId = 0;
    }

    public String createUser(String name, String mobile) throws Exception {
        if(userMobile.contains(mobile)){
            throw new Exception("User already exists");
        }
        User user = new User(name, mobile);
        userMobile.add(mobile);
        return "New User created !!";
    }

    public Group createGroup(List<User> users) {
        Group group = new Group();

        if(users.size() == 2){
            group.setName(users.get(1).getName());
        }
        else{
            customGroupCount += 1;
            group.setName("Group "+customGroupCount);
        }

        User admin = users.get(0);

        group.setNumberOfParticipants(users.size());
        groupUserMap.put(group, users);
        adminMap.put(group, admin);

        return group;
    }

    public int createMessage(String content) {
        messageId += 1;
        Message message = new Message(messageId, content);
        Date date = new Date();

        return message.getId();
    }

    public int sendMessage(Message message, User sender, Group group) throws Exception {
        if(!groupUserMap.containsKey(group)) throw new Exception("Group does not exist");

        List<User> groupMembers = groupUserMap.get(group);
        if(!groupMembers.contains(sender)) throw new Exception("You are not allowed to send message");

        List<Message> messageList = groupMessageMap.getOrDefault(group,new ArrayList<>());
        messageList.add(message);
        groupMessageMap.put(group, messageList);

        senderMap.put(message, sender);

        return groupMessageMap.get(group).size();
    }

    public String changeAdmin(User approver, User user, Group group) throws Exception {
        if(!adminMap.containsKey(group)) throw new Exception("Group does not exist");

        if(!adminMap.containsKey(approver)) throw new Exception("Approver does not have rights");

        List<User> members = groupUserMap.get(group);
        if(!members.contains(user)) throw new Exception("User is not a participant");

        adminMap.put(group, user);
        return "SUCCESS";
    }

    public int removeUser(User user) throws Exception {
        //A user belongs to exactly one group
        //If user is not found in any group, throw "User not found" exception
        //If user is found in a group and it is the admin, throw "Cannot remove admin" exception
        //If user is not the admin, remove the user from the group, remove all its messages from all the databases, and update relevant attributes accordingly.
        //If user is removed successfully, return (the updated number of users in the group + the updated number of messages in group + the updated number of overall messages)

        boolean found = false;
        Group userGroup = new Group();

        for(Group group : groupUserMap.keySet()){
            if(groupUserMap.get(group).contains(user)){
                found = true;
                userGroup = group;
                break;
            }
        }
        if(!found) throw new Exception("User not found");

        if(adminMap.get(userGroup) == user) throw new Exception("Cannot remove admin");

        List<User> users = groupUserMap.get(userGroup);
        users.remove(user);
        groupUserMap.put(userGroup, users);
        userGroup.setNumberOfParticipants(users.size());

        List<Message> messages = groupMessageMap.get(userGroup);
        for(Message message : messages){
            User sender = senderMap.get(message);
            if(sender == user){
                messages.remove(message);
                senderMap.remove(message);
            }
        }
        groupMessageMap.put(userGroup, messages);

        int ans = groupUserMap.get(userGroup).size() +
                  groupMessageMap.get(userGroup).size() +
                  senderMap.size();

        return ans;
    }

    public String findMessage(Date start, Date end, int k) throws Exception {
        List<Message> messages = new ArrayList<>();
        for(Group group: groupMessageMap.keySet()){
            messages.addAll(groupMessageMap.get(group));
        }
        List<Message> filteredMessages = new ArrayList<>();
        for(Message message: messages){
            if(message.getTimestamp().after(start) && message.getTimestamp().before(end)){
                filteredMessages.add(message);
            }
        }
        if(filteredMessages.size() < k){
            throw new Exception("K is greater than the number of messages");
        }
        Collections.sort(filteredMessages, new Comparator<Message>(){
            public int compare(Message m1, Message m2){
                return m2.getTimestamp().compareTo(m1.getTimestamp());
            }
        });


        return filteredMessages.get(k-1).getContent();
    }
}
