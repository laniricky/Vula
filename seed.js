const admin = require('firebase-admin');

// Connect to local emulator
process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8088';
process.env.FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099';

admin.initializeApp({
  projectId: 'vula-6d723'
});

const db = admin.firestore();
const auth = admin.auth();

const dummyData = [
  { name: 'Alice', phone: '1234567890' },
  { name: 'Bob', phone: '0987654321' },
  { name: 'Charlie', phone: '1112223333' }
];

async function seed() {
  console.log('Seeding data to local emulator...');

  for (let i = 0; i < dummyData.length; i++) {
    const data = dummyData[i];
    const username = data.name.toLowerCase();
    const email = `${data.phone}@vula.local`;
    const password = 'password123';

    try {
      // 1. Create User in Auth
      const userRecord = await auth.createUser({
        uid: `user_${i}`,
        email: email,
        password: password,
        displayName: data.name,
      });

      console.log(`Created user: ${username} (${userRecord.uid})`);

      // 2. Create User document in Firestore
      await db.collection('users').doc(userRecord.uid).set({
        id: userRecord.uid,
        username: username,
        phoneNumber: data.phone,
        displayName: data.name,
        createdAt: Date.now()
      });

      // 3. Create Username reference
      await db.collection('usernames').doc(username).set({
        userId: userRecord.uid
      });

      // 4. Create a Dummy Post for the user
      const postRef = db.collection('posts').doc();
      await postRef.set({
        id: postRef.id,
        authorId: userRecord.uid,
        authorUsername: username,
        caption: `Hello from ${data.name}! This is a dummy post to test the Vula Feed.`,
        createdAt: Date.now() - (i * 100000), // Stagger timestamps
        likesCount: 0,
        commentsCount: 0,
        likedBy: []
      });

      console.log(`Created post for ${username}`);

    } catch (error) {
      console.error(`Error creating data for ${data.name}:`, error.message);
    }
  }

  console.log('Seeding complete!');
  process.exit(0);
}

seed();
