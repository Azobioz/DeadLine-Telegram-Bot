Как запустить телеграм бота
 -----
Далее будут предоставлены запуски бота на разных операционных системах

Требования:
-----
* Docker Desktop (и Docker Compose, если скачиваете на Linux). Установить можно [тут](https://www.docker.com/get-started/#h_installation)

На Windows
-----
1. Нажмите на файл docker-compose.yaml 
![](https://i.ibb.co/Q7x5VkYs/1-docker-compose.png)   

2. Нажмите на кнопку с 3 точками справа верхнем углу и нажмите Download

   ![](https://i.ibb.co/NdyLc9R1/2-docker-compose.png)

3. Загрузите файл в удобную вам директорию
4. Откройте cmd или powershell
5. Перейдите в директорию, где находится docker-compose.yaml

       cd C:\какая\то\директория\где\находится\docker-compose.yaml
   
6. Запустите телеграм бота через docker в консоли:

       docker compose up -d

7. Перейдите в телеграме к боту с никнеймом Dl_Dead_Line_bot и воаля, пользуйтесь :)
8. Чтобы выключить бота пропишите в той-же консоли эту команду или если закрыли консоль, то выполните шаги 4 и 5 и пропишите команду ниже:

        docker compose down

На Mac
-----
1. Нажмите на файл docker-compose.yaml 
![](https://i.ibb.co/Q7x5VkYs/1-docker-compose.png)   

2. Нажмите на кнопку с 3 точками справа верхнем углу и нажмите Download

   ![](https://i.ibb.co/NdyLc9R1/2-docker-compose.png)

3. Загрузите файл в удобную вам директорию
4. Нажмите Cmd + T или найдите Терминал через Spotlight.
5. Перейдите в директорию, где находится docker-compose.yaml

        cd C:\какая\то\директория\где\находится\docker-compose.yaml

6. Запустите телеграм бота через docker в консоли:

       docker compose up -d

7. Перейдите в телеграме к боту с никнеймом Dl_Dead_Line_bot и воаля, пользуйтесь :)
8. Чтобы выключить бота пропишите в той-же консоли эту команду или если закрыли консоль, то выполните шаги 4 и 5 и пропишите команду ниже:

       docker compose down

На Linux
-----
1. Откройте терминал и перейдите в директорию куда вы бы хотели загрузить docker-compose.yaml

       cd C:\какая\то\директория\куда\я\хочу\загрузить
   
2. Используйте команду wget или curl в терминале для загрузки файла
   
       wget https://raw.githubusercontent.com/Azobioz/DeadLine-Telegram-Bot/refs/heads/main/docker-compose.yaml -O docker-compose.yaml
   
   или

       curl -o docker-compose.yaml https://raw.githubusercontent.com/Azobioz/DeadLine-Telegram-Bot/refs/heads/main/docker-compose.yaml
3. Запустите docker-compose.yaml в той же директории, где он находится

       docker compose up -d

4. Перейдите в телеграме к боту с никнеймом Dl_Dead_Line_bot и воаля, пользуйтесь :)
5. Чтобы выключить бота пропишите в том же терминале эту команду или если закрыли терминал, то выполните шаг 1 и пропишите команду ниже:

       docker compose down
