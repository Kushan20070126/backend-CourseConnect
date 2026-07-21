// Run this once while connected as an existing MongoDB administrator:
// mongosh --authenticationDatabase admin -u <admin-user> -p admin scripts/create-courseconnect-user.js
// MongoDB will prompt for the new application's password; it is never stored here.

const appDatabase = db.getSiblingDB("courseconnect_content");
const username = "courseconnect_app";
const password = passwordPrompt();

if (appDatabase.getUser(username)) {
  appDatabase.updateUser(username, {
    pwd: password,
    roles: [{ role: "readWrite", db: "courseconnect_content" }]
  });
  print(`Updated password for ${username}.`);
} else {
  appDatabase.createUser({
    user: username,
    pwd: password,
    roles: [{ role: "readWrite", db: "courseconnect_content" }]
  });
  print(`Created ${username}.`);
}
