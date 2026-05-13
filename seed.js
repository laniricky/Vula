#!/usr/bin/env node
/**
 * seed.js — Vula Docker backend seed script
 *
 * Seeds the PostgreSQL database via the Go REST API with realistic dummy data:
 *   • 8 users with avatars (Unsplash URLs)
 *   • ~3 posts per user (mix of images and videos)
 *   • Stories for 5 users
 *   • Follow relationships
 *   • Chat rooms + messages
 *
 * Usage: node seed.js
 * Requires: node-fetch (npm i node-fetch@2)
 */

const fetch = require('node-fetch');

const BASE = process.env.VULA_API || 'http://localhost:8081';

// ── Dummy users ───────────────────────────────────────────────────────────────

const USERS = [
  { phone: '+254700000001', username: 'amara_ke',   displayName: 'Amara Njeri',    bio: 'Nairobi sunsets 🌅 | Travel | Food',          avatar: 'https://i.pravatar.cc/300?img=47' },
  { phone: '+254700000002', username: 'juma_tz',    displayName: 'Juma Hassan',    bio: 'DJ | Music Producer 🎵 | Dar es Salaam',       avatar: 'https://i.pravatar.cc/300?img=12' },
  { phone: '+254700000003', username: 'zawadi_ug',  displayName: 'Zawadi Auma',    bio: 'Fashion & Lifestyle ✨ | Kampala',              avatar: 'https://i.pravatar.cc/300?img=32' },
  { phone: '+254700000004', username: 'kofi_gh',    displayName: 'Kofi Mensah',    bio: 'Tech entrepreneur | Accra 🇬🇭',                avatar: 'https://i.pravatar.cc/300?img=51' },
  { phone: '+254700000005', username: 'adaeze_ng',  displayName: 'Adaeze Okonkwo', bio: 'Writer & poet 📝 | Lagos lit scene',            avatar: 'https://i.pravatar.cc/300?img=44' },
  { phone: '+254700000006', username: 'sipho_za',   displayName: 'Sipho Ndlovu',   bio: 'Football ⚽ | Cape Town',                      avatar: 'https://i.pravatar.cc/300?img=15' },
  { phone: '+254700000007', username: 'fatou_sn',   displayName: 'Fatou Diallo',   bio: 'Art & culture 🎨 | Dakar',                     avatar: 'https://i.pravatar.cc/300?img=38' },
  { phone: '+254700000008', username: 'chidi_ng',   displayName: 'Chidi Obi',      bio: 'Street photographer 📸 | Abuja',               avatar: 'https://i.pravatar.cc/300?img=22' },
];

// ── Post content ──────────────────────────────────────────────────────────────

const POSTS = [
  { caption: 'Nairobi at magic hour 🌇 #nairobi #kenya #travel', imageUrl: 'https://images.unsplash.com/photo-1611348586755-53ff1c19fa95?w=800', mediaType: 'image' },
  { caption: 'New track dropping Friday 🔥 #afrobeats #music #dj', imageUrl: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=800', mediaType: 'image' },
  { caption: 'Street style Kampala 💅 #fashion #kampala #africa', imageUrl: 'https://images.unsplash.com/photo-1509631179647-0177331693ae?w=800', mediaType: 'image' },
  { caption: 'Building the future from Accra 🚀 #tech #startup #ghana', imageUrl: 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800', mediaType: 'image' },
  { caption: 'Words have power ✍️ #poetry #writing #lagos #literature', imageUrl: 'https://images.unsplash.com/photo-1455390582262-044cdead277a?w=800', mediaType: 'image' },
  { caption: 'Sunday league goals only ⚽ #football #capetown #southafrica', imageUrl: 'https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=800', mediaType: 'image' },
  { caption: 'Colors of Dakar 🎨 #art #senegal #africa #culture', imageUrl: 'https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=800', mediaType: 'image' },
  { caption: 'Abuja by night 📷 #photography #abuja #nigeria #streetphotography', imageUrl: 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800', mediaType: 'image' },
  { caption: 'Chapati & chai for breakfast ☕ #kenyanfood #foodie #nairobi', imageUrl: 'https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=800', mediaType: 'image' },
  { caption: 'Sold out show!! 🙏🎶 #music #concert #tanzania #vibes', imageUrl: 'https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?w=800', mediaType: 'image' },
  { caption: 'Thrift flip 🔥 before & after #fashion #thrift #sustainability', imageUrl: 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800', mediaType: 'image' },
  { caption: 'Hackathon weekend 💻 72hrs no sleep #coding #accra #tech', imageUrl: 'https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=800', mediaType: 'image' },
  { caption: 'The anthology is coming 📚 #books #poetry #writersofinstagram', imageUrl: 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=800', mediaType: 'image' },
  { caption: 'Golden Boot season 🏆 #football #goals #capetown', imageUrl: 'https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=800', mediaType: 'image' },
  { caption: 'Teranga vibes always 🤝 #senegal #teranga #community', imageUrl: 'https://images.unsplash.com/photo-1603052875302-d376b7c0638a?w=800', mediaType: 'image' },
  { caption: 'Light and shadow 📸 #photooftheday #streetphotography #nigeria', imageUrl: 'https://images.unsplash.com/photo-1496440737103-cd596325d314?w=800', mediaType: 'image' },
  { caption: 'Safari morning 🦒 #kenya #safari #wildlife #naturephotography', imageUrl: 'https://images.unsplash.com/photo-1516426122078-c23e76319801?w=800', mediaType: 'image' },
  { caption: 'Studio session at 3am 🎙️ #producer #music #grind #afrobeats', imageUrl: 'https://images.unsplash.com/photo-1598653222000-6b7b7a552625?w=800', mediaType: 'image' },
  { caption: 'Kente print season 🧵 #ghana #fashion #kente #africanprint', imageUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800', mediaType: 'image' },
  { caption: 'Reading list for the weekend 📖 #books #lagos #readersofinstagram', imageUrl: 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=800', mediaType: 'image' },
  { caption: 'Catch of the day 🎣 #fishing #capetown #ocean #lifestyle', imageUrl: 'https://images.unsplash.com/photo-1559827291-72ebdfca54b5?w=800', mediaType: 'image' },
  { caption: 'Goree Island at dusk 🌊 #senegal #goree #heritage #travel', imageUrl: 'https://images.unsplash.com/photo-1541480601022-2308c0f02487?w=800', mediaType: 'image' },
  { caption: 'City portraits 🌃 #abuja #portrait #photography #urban', imageUrl: 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=800', mediaType: 'image' },
  { caption: 'Lake Naivasha weekend escape 🚣 #kenya #travel #nature', imageUrl: 'https://images.unsplash.com/photo-1564419320461-6870880221ad?w=800', mediaType: 'image' },
  // ── Video posts — all URLs verified 200 OK ───────────────────────────────
  { caption: 'Nairobi skyline magic hour 🌇 #nairobi #video #africa',   videoUrl: 'https://www.w3schools.com/html/mov_bbb.mp4',                                                        mediaType: 'video' },
  { caption: 'Studio vibes in Dar es Salaam 🎥 #music #bts #afrobeats', videoUrl: 'https://media.w3.org/2010/05/sintel/trailer.mp4',                                                   mediaType: 'video' },
  { caption: 'Kampala street life 🇺🇬 #uganda #city #vlog',             videoUrl: 'https://media.w3.org/2010/05/bunny/movie.mp4',                                                      mediaType: 'video' },
  { caption: 'Hackathon pitch day 🚀 #accra #tech #startup',            videoUrl: 'https://vjs.zencdn.net/v/oceans.mp4',                                                               mediaType: 'video' },
  { caption: 'Lagos book launch 📚 #poetry #nigeria #culture',          videoUrl: 'https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4',                  mediaType: 'video' },
  { caption: 'Dakar sunset dance 💃 #senegal #dance #vibes',            videoUrl: 'https://www.w3schools.com/html/movie.mp4',                                                           mediaType: 'video' },
];

// ── Chat messages ─────────────────────────────────────────────────────────────

const MESSAGES = [
  'Mambo! 👋',
  'Sawa sana, wewe?',
  'Karibu Nairobi next week?',
  'Niko tayari! 🔥',
  'Tutaonana! 🤝',
  'Your last post was 🔥🔥',
  'Thanks bro! Working on something new',
  'Can\'t wait to see it! 👀',
  'I saw your story, where is that place?',
  'Mombasa! Come through 🌊',
];

// ── Helpers ───────────────────────────────────────────────────────────────────

async function post(path, body, token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  });
  return res.ok ? res.json().catch(() => ({})) : null;
}

async function put(path, body, token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(`${BASE}${path}`, { method: 'PUT', headers, body: JSON.stringify(body) });
  return res.ok ? res.json().catch(() => ({})) : null;
}

async function get(path, token) {
  const headers = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(`${BASE}${path}`, { headers });
  return res.ok ? res.json().catch(() => null) : null;
}

// ── Main seeder ───────────────────────────────────────────────────────────────

async function seed() {
  console.log(`\n🌱  Seeding Vula backend at ${BASE}\n`);

  const sessions = [];

  // 1. Create users (request + verify OTP)
  for (const user of USERS) {
    await post('/api/auth/request-code', { phone: user.phone });
    // The Go backend logs the OTP when Twilio is unconfigured — use 123456 as dev default
    const verifyRes = await post('/api/auth/verify-code', { phone: user.phone, code: '123456' });
    if (!verifyRes) { console.error(`❌  Auth failed for ${user.phone}`); continue; }

    const { token, userId } = verifyRes;
    sessions.push({ ...user, token, userId });

    // Update profile
    await put('/api/users/me', {
      displayName:     user.displayName,
      username:        user.username,
      bio:             user.bio,
      richStatus:      '',
      website:         '',
      isPrivate:       false,
      profileImageUrl: user.avatar,
      bannerUrl:       user.banner || 'https://images.unsplash.com/photo-1506744626753-1fa30f9cb285?w=800'
    }, token);

    console.log(`✅  Created user: @${user.username} (${userId})`);
  }

  if (sessions.length === 0) {
    console.error('No sessions created — is the backend running?');
    process.exit(1);
  }

  // 2. Create posts — distribute POSTS array round-robin across users
  for (let i = 0; i < POSTS.length; i++) {
    const session = sessions[i % sessions.length];
    const p       = POSTS[i];

    // POST body — backend accepts JSON for caption + mediaType; imageUrl/videoUrl provided as URL
    const body = {
      caption:   p.caption,
      mediaType: p.mediaType,
      imageUrl:  p.imageUrl  || null,
      videoUrl:  p.videoUrl  || null,
    };
    await post('/api/posts', body, session.token);
    process.stdout.write(p.mediaType === 'video' ? '🎬 ' : '📸 ');
  }
  console.log(`\n✅  Created ${POSTS.length} posts`);

  // 3. Create stories for first 5 users
  const storyImageUrls = [
    'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=600',
    'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=600',
    'https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=600',
    'https://images.unsplash.com/photo-1504257432389-52343af06ae3?w=600',
    'https://images.unsplash.com/photo-1567653418876-5bb0e566e1c2?w=600',
  ];
  for (let i = 0; i < 5 && i < sessions.length; i++) {
    await post('/api/stories', {
      imageUrl:  storyImageUrls[i],
      mediaType: 'image',
    }, sessions[i].token);
    process.stdout.write('📖 ');
  }
  console.log(`\n✅  Created 5 stories`);

  // 4. Follow relationships (each user follows the next 3)
  for (let i = 0; i < sessions.length; i++) {
    for (let j = 1; j <= 3; j++) {
      const target = sessions[(i + j) % sessions.length];
      await post(`/api/users/${target.userId}/follow`, {}, sessions[i].token);
      process.stdout.write('🤝 ');
    }
  }
  console.log(`\n✅  Created follow relationships`);

  // 5. Create a direct chat between user[0] and user[1] with messages
  if (sessions.length >= 2) {
    const roomRes = await post('/api/chat/rooms/direct', { otherUserId: sessions[1].userId }, sessions[0].token);
    if (roomRes?.roomId) {
      for (let i = 0; i < MESSAGES.length; i++) {
        const sender = i % 2 === 0 ? sessions[0] : sessions[1];
        await post(`/api/chat/rooms/${roomRes.roomId}/messages`, { text: MESSAGES[i] }, sender.token);
        process.stdout.write('💬 ');
      }
      console.log(`\n✅  Created chat room with ${MESSAGES.length} messages`);
    }
  }

  console.log('\n🎉  Seed complete! The app is ready for demo.\n');
  console.log('Users:');
  sessions.forEach(s => console.log(`  @${s.username.padEnd(14)} token: ${s.token.slice(0, 30)}...`));
  process.exit(0);
}

seed().catch(err => { console.error(err); process.exit(1); });
