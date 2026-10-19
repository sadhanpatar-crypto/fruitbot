# Fruit Bot (Android)

Ye app screen dekhta hai. Knife ke raaste (screen ke beech ki ek patli patti) mein koi
bright, rangeen cheez (fruit) aaye to wo screen par tap kar deta hai, aur knife chhoot jaata hai.

## APK kaise banaye

### Tareeka A: GitHub se (Android Studio nahi chahiye)
1. github.com par free account banao, ek naya repository banao (jaise `fruitbot`).
2. Is folder ki saari files (`.github` folder samet) us repository mein upload karo.
   `.github/workflows/build.yml` zaroor jaani chahiye.
3. Repository ke **Actions** tab mein jao. "Build APK" apne aap chalega
   (na chale to "Run workflow" dabao). 3-5 minute lagte hain.
4. Run khatam hone par neeche **Artifacts** mein `FruitBot-apk` download karo.
   Zip kholo, andar `app-debug.apk` hai. Use phone mein bhej do.

### Tareeka B: Android Studio se
1. Android Studio mein **Open** karke is folder ko kholo, Gradle sync hone do.
2. **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
3. APK yahan milega: `app/build/outputs/apk/debug/app-debug.apk`

## Phone par setup
1. APK install karo (Unknown sources allow karna pad sakta hai).
2. **Android 13 ya upar:** Settings > Apps > Fruit Bot > teen dot (⋮) > **Allow restricted settings**.
   (Ye na karo to Accessibility mein Fruit Bot grey dikhega.)
3. App kholo > **1. Accessibility chalu karo** > Fruit Bot dhundo > ON.
4. App mein wapas aao > **2. Bot Start** > "Start now" dabao.
5. Game kholo aur khelo. Bot apne aap tap karega.
6. Rokne ke liye notification mein **Stop** dabao ya app mein **Bot Stop**.

## Tuning (zaroori hai, har phone/game ke liye alag)
Notification mein `pixels=...` dikhta hai: patti mein kitne "fruit jaise" pixel hain.
- Jab patti khaali ho to `pixels` kitna aata hai? (jaise 0-5)
- Jab fruit patti se guzre to kitna? (jaise 200+)
- **Sensitivity** ko in dono ke beech rakho.

Bomb se bachna: notification mein `bomb=kala/laal` dikhta hai (patti mein kitne kale aur gahre laal pixel hain).
Bot tab hi bomb maanta hai jab dono ek saath hon (kala >= Bomb sensitivity aur laal >= 10).
Bomb dikhne par bot 400 ms tak tap nahi karta. Seb mein kala rang nahi hota, isliye seb par tap hota rahega.
- Bomb par tap ho jaye to **Bomb sensitivity** kam karo.
- Seb/fruit par tap band ho jaye to **Bomb sensitivity** badhao.

Sliders:
- **Patti ki jagah (X):** knife ke seedhe upar rakho. Aapke screenshot mein knife lagbhag 50% par hai.
- **Neeche ka kinara:** knife ke handle se thoda upar. Knife khud rangeen hai, usse bot ko na pakadna chahiye.
- **Upar ka kinara:** upar ki TIME wali patti se neeche.
- **Gap:** bahut kam rakha to ek hi fruit par do baar tap hoga.
- **Tap ki height:** game mein jahan tap karne se knife chhoot-ta hai.

Agar bot kabhi late tap kare to patti ka neeche ka kinara upar karo (fruit jaldi pakda jayega).
Agar bina fruit ke tap kare to Sensitivity badhao.

## Dhyan do
- Ye app Google Play ya kisi anti-cheat wale online game par mat chalana. Offline/normal game ke liye hai.
- Code yahan compile karke test nahi kiya gaya. Pehli baar build mein koi error aaye to error bhej dena.
