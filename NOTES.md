Bug 1: SQL AND/OR precedence
1. Where: TaskRepository.java, @Query line 14. Same in
   search_tasks.sql and task_search_package.sql (2 places).
   Layer: SQL.
2. How I found it: I chose status=DONE in the API and read
   the query. I saw AND and OR mixed without brackets.
3. Root cause: AND runs before OR. So the query meant
   (archived=false AND title match) OR (description match
   AND status match). Archived tasks and wrong status
   tasks came in through the description match.
4. Fix and why: I put brackets around the title and
   description match. Now archived and status always apply.
Bug 2: Unnecessary Thread.sleep()
1. Where: TaskController.java (the "complexity" part). Layer: backend.
2. How I found it: I read the controller code and saw Thread.sleep().
3. Root cause: The code waited on purpose, delay = (10 - search length) * 100 ms.
   An empty search waited 1 second. It did no useful work and only slowed the response.
4. Fix and why: I removed the Thread.sleep() code, so the API responds faster.

Bug 3: Loading never stops / old response overwrites new response
1. Where: useTasks.js. Layer: frontend.
2. How I found it: (A) When the API request failed, "Loading tasks..." never went away.
   (B) I reasoned from the code that typing "a" then "ap" sends two requests.
3. Root cause: (A) setLoading(false) was not always called, so a failed request left
   loading on. (B) If the first request finished later than the second, the old
   response overwrote the new one.
4. Fix and why: (A) I used finally cleanup logic so loading always stops.
   (B) I used a cancelled flag so old requests cannot update the screen.

Bug 4: Page does not reset to page 1
1. Where: App.jsx. Layer: frontend.
2. How I found it: I searched while on page 3 and got no records, even though
   records existed.
3. Root cause: The page number was not reset when the search or status filter changed.
   The new search had only 1 page, but the app still asked for page 3.
4. Fix and why: I added setPage(1) when search or status changes, so results
   always start from page 1.

Bug 5: Backend input validation
1. Where: TaskController.java. Layer: backend.
2. How I found it: I tested invalid values such as status=abc and page=0 and
   saw an error response.
3. Root cause: The backend did not validate user input properly, so bad values
   could cause a 500 server error.
4. Fix and why: I added validation: status must be a valid TaskStatus value,
   page must be 1 or more, pageSize must be between 1 and 100. Invalid input
   returns 400 Bad Request with a clear error message.

Bug 6: SQL LIKE wildcards
1. Where: TaskController.java (search input handling). Layer: backend.
2. How I found it: I searched with % and saw all records, even unrelated ones.
3. Root cause: In SQL LIKE, % means any number of characters and _ means one
   character. These special characters were not escaped.
4. Fix and why: I escaped \, % and _ before using the text in the LIKE query,
   so user input is treated as normal text.
