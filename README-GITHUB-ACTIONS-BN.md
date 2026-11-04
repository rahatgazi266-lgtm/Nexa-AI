# Nexa AI — মোবাইল থেকে বিনামূল্যে Release APK তৈরি

এই workflow বিদ্যমান Android project structure, UI, applicationId এবং source attribution বদলায় না। GitHub Actions-এ signed APK তৈরি হয় এবং Artifact হিসেবে পাওয়া যায়। `v*` tag push করলে ঐ GitHub Release-এ APK যুক্ত হয়।

## যা আগে লাগবে

- GitHub-এ `Nexa-AI` repository (Public repository হলে GitHub-hosted Actions-এর standard minutes বিনামূল্যে; usage limits প্রযোজ্য)।
- একটি নিজস্ব Android upload keystore। এটি একবার তৈরি করতে হবে। একই keystore ভবিষ্যতের আপডেটেও সংরক্ষণ করতে হবে। হারালে পুরোনো ইনস্টলেশনের ওপর update বসাতে সমস্যা হতে পারে।
- GitHub repository-র Settings → Secrets and variables → Actions-এ নিচের Secrets:
  - `KEYSTORE_BASE64`: `.jks` ফাইলের Base64 content
  - `STORE_PASSWORD`: keystore password
  - `KEY_PASSWORD`: `upload` alias-এর key password
  - `EXPECTED_CERT_SHA256`: (ঐচ্ছিক কিন্তু প্রস্তাবিত) upload certificate SHA-256 hex, colon ছাড়া
  - `GEMINI_API_KEY`: প্রজেক্টের build-time secret সত্যিই প্রয়োজন হলে; না দিলে key ছাড়া build হবে

কোনো password, keystore বা API key Git commit, ZIP, README, issue বা chat-এ রাখবে না। শুধু GitHub Actions Secrets-এ রাখবে।

## ফোনে keystore তৈরি করার সবচেয়ে নিরাপদ উপায়

Android Studio-র **Build → Generate Signed Bundle / APK → APK → Create new** ব্যবহার করে একবার upload keystore তৈরি করো (কম্পিউটার লাগবে)। Keystore তৈরি হলে ফাইলটি অফলাইনে নিরাপদ জায়গায় অন্তত দুইটি encrypted backup রাখো। ফোনে Termux/অনির্ভরযোগ্য ওয়েবসাইটে keystore password টাইপ করে তৈরি করার পরামর্শ দেওয়া হচ্ছে না।

Keystore আগে থেকেই থাকলে কম্পিউটারে Base64 value তৈরি করা যায়:

```bash
base64 -w 0 upload-key.jks
```

এর সম্পূর্ণ output `KEYSTORE_BASE64` Secret-এ দাও। Keystore alias অবশ্যই `upload` হবে, কারণ বর্তমান Gradle config-এ alias সেটাই। যদি alias আলাদা হয়, তখন project-এর signing config-এ alias পরিবর্তন প্রয়োজন; এখানে source বদলানো হয়নি।

Certificate SHA-256 বের করতে:

```bash
keytool -list -v -keystore upload-key.jks -alias upload
```

`SHA256` fingerprint-এর colon বাদ দিয়ে `EXPECTED_CERT_SHA256` Secret-এ রাখো।

## GitHub mobile app / browser দিয়ে workflow যোগ করা

1. `Nexa-AI` repository খোলো।
2. **Add file → Create new file** নির্বাচন করো।
3. Filename দাও `.github/workflows/android-release.yml`।
4. এই ZIP-এর একই path-এ থাকা workflow ফাইলের সম্পূর্ণ content paste করে commit করো।
5. Repository → **Settings → Secrets and variables → Actions → New repository secret** থেকে উপরের Secrets যোগ করো।
6. Repository → **Actions → Nexa AI Android Release APK → Run workflow** চালাও। অথবা `v2.0.0` tag push করলে automation চালু হবে।
7. Run শেষ হলে Actions run-এর **Artifacts** থেকে `Nexa-AI-Release-APK` ডাউনলোড করো।

## GitHub Release-এ যুক্ত করা

`v2.0.0` tag আগে থেকে Release Draft-এ থাকলে workflow tag trigger করে APK upload করতে পারবে; Release draft থাকা অবস্থায় CLI upload asset attach করতে পারে। Publish করার আগে Asset list-এ `app-release.apk` উপস্থিত কিনা যাচাই করো। `gh release upload` GitHub CLI-র API ব্যবহার করে।

## গুরুত্বপূর্ণ সতর্কতা

- Workflow-তে Java 21 এবং Android API 36 ব্যবহার করা হয়েছে; Gradle Wrapper নিজস্ব version `9.3.1`-ই ব্যবহার করে।
- বর্তমান `applicationId` অপরিবর্তিত: `com.aistudio.nexaai.hqzjtr`।
- বর্তমান `versionName`-ও অপরিবর্তিত `1.4`; GitHub Release tag `v2.0.0` Android app-এর versionName নিজে থেকে বদলায় না।
- বর্তমান Gradle signing config-এ `keyAlias = "upload"`। নিজের keystore alias ভিন্ন হলে signing ব্যর্থ হবে।
- `EXPECTED_CERT_SHA256` secret না দিলে workflow signed APK-এর internal signature validity যাচাই করে, কিন্তু নির্দিষ্ট certificate pinning তুলনা skip করে। Production release-এর আগে এটি অবশ্যই সেট করো।
- Build workflow `testDebugUnitTest` ও `assembleRelease` চালায়। এগুলো pass না করলে APK artifact তৈরি বা publish হবে না।
- Android Keystore-এর app runtime storage security আলাদা বিষয়; এই build workflow সেটি প্রমাণ করে না। প্রকৃত Android device instrumentation test আলাদা করে চালাতে হবে।
- Release APK-তে Gemini API key embed করা হলে সেটি APK থেকে বের করা সম্ভব। Public APK-তে shared server API key embed না করে backend proxy ব্যবহার করো, অথবা ব্যবহারকারীর নিজস্ব key secure storage-এ রাখার ব্যবস্থা করো।
