import pandas as pd
import matplotlib.pyplot as plt

df = pd.read_csv('tree_time.csv', header=None, names=['processes', 'time'])

avg_times = df.groupby('processes')['time'].mean()

plt.figure(figsize=(10, 6))
plt.plot(avg_times.index, avg_times.values, marker='o', linestyle='-', color='b')
plt.xlabel('Количество процессов')
plt.ylabel('Среднее время выполнения (среднее из 10 замеров)')
plt.title('Зависимость времени выполнения от количества процессов')
plt.grid(True)
plt.xticks(avg_times.index)
plt.show()
