# Maximal Cardinality Matching Solver (Large-Scale Data Processing)
## Abstract
This project aims to design and develop a scalable and parallelizable algorithm that can maximize the matching size of a given dataset. This project aims to resolve the issues of the Israeli-Itai algorithm and the Greedy Random Matching algorithm by combining the advantages of the two algorithms and enhancing the size of the matched set through a scalable algorithm that searches and converts augmenting paths of length 3.

Keywords -- Maximal Cardinality Matching, Israeli-Itai, Greedy Random Matching, Fallback, Augmenting Path, RDD, GraphX, Scala, Apache Spark, Parallel Computing

## I. Introduction
The Israeli-Itai algorithm is an effective and scalable algorithm in forming a valid matched set in large-scale data, as it utilizes parallel message exchange among vertices. However, when implemented, there exists a discrepancy between the theoretical and empirical time-complexity due to how the algorithm determines which of the proposals will be accepted (generating random bits and breaking symmetry). To investigate this problem further, the Israeli-Itai algorithm was implemented to produce output files after each iteration, showing how many edges were added to the matched set in the given iteration.

## II. Hybrid Algorithm Utilizing Fallback to Greedy Random Matching
The hybrid algorithm designed for this project utilizes the advantages of Israeli-Itai algorithm and Greedy Random Matching. When the algorithm is executed, the Israeli-Itai algorithm is applied to the graph, creating proposals and forming a matched set through iteration. After a certain number of iterations (depending on the density and the degree of the graph), the graph will reach a point where Israeli-Itai algorithm begins to perform iterations in which there are no additional matches added to the set. Whenever this occurs, the zeroStreak value in incremented by one. When the zeroStreak reaches the threshold value (arbitrarily decided to be 3 in this repository), the program performs a fallback to Greedy Random Matching algorithm, forming matches among the remaining edges and vertices in a greedy manner.

## III. Maximal Cardinality Matching Results & Analysis
### Input Data
| Input File Name             | Number of Edges |
| --------------------------- | --------------- |
| soc-pokec-relationships.csv | 22,301,964      |
| soc-LiveJournal1.csv        | 42,851,237      |
| twitter_original_edges.csv  | 63,555,749      |
| com-orkut.ungraph.csv       | 117,185,083     |

### Results
| File Name                   | Matching Size | Approach              | Runtime (Local) | Runtime (GCP 4x2 Cores) | Iterations (+ Greedy) |
| --------------------------- | ------------- | --------------------- | --------------- | ----------------------- | --------------------- |
| soc-pokec-relationships.csv | 599,530       | Israeli-Itai + Greedy | 7m 57s          | 10m 36s                 | 40 + 2                |
| soc-LiveJournal1            | 1,578,566     | Israeli-Itai + Greedy | 16m 37m         | 20m 7s                  | 42 + 2                |
| twitter_original_edges      | 92,404        | Israeli-Itai + Greedy | 20m 40s         | 38m 16s                 | 27 + 0                |
| com-orkut.ungraph.csv       | 1,339,741     | Israeli-Itai + Greedy | 38m 10s         | 40m 43s                 | 42 + 3                |

### Analysis
| Algorithm              | Time-Complexity  | Space-Complexity | Scalability            | Parellelization |
| ---------------------- | ---------------- | ---------------- | ---------------------- | --------------- |
| Israeli-Itai           | O(E) $\times$ k  | O(V + E)         | Yes                    | Yes             |
| Greedy Random Matching | O(E') $\times$ l | O(V + E')        | Yes (when E' is small) | No              |
| Augmenting Path        | O(E + plog(p)    | O(m + n + p)     | Yes                    | Yes             |

Legend:
V = Number of Vertices
V' = Number of Vertices Left
E = Number of Edges
k = Number of Israeli-Itai Iterations
l = Number of Greedy Random Matching Iterations
p = Number of Augmenting Paths FOUND

### Advantages
| Algorithm              | Scalable | Parallelizable | Matching Chance*          |
| ---------------------- | -------- | -------------- | ------------------------- |
| Israeli-Itai           | Yes      | Yes            | 1/d $\times$ 1/4          |
| Greedy Random Matching | No       | No             | 1/d                       |
| Hybrid Algorithm       | Yes      | Yes            | 1/d or 1/d $\times$ 1/4** |

\* Chance of a given vertex being eliminated </br>
** Depending on how sparce the remaining vertices are (zeroStreak)

## IV. Enhancing Results by Searching and Converting Augmenting Paths


## V. Augmenting Path Enhancement Results & Analysis

| File Name                   | Original Matching | After 1 Iteration | After (n) Iterations | Runtime per Iteration |
| --------------------------- | ----------------- | ----------------- | -------------------- | --------------------- |
| soc-pokec-relationships.csv | 599,530           | 623,483           | 703,095 (16)         | 1-2 minutes           |
| soc-LiveJournal1            | 1,578,566         | 1,692,282         | 1,890,074 (10)       | 2-4 minutes           |
| twitter_original_edges      | 92,404            | N/A               | N/A                  | N/A                   |
| com-orkut.ungraph.csv       | 1,339,741         | 1,363,212         | 1,461,419 (10)       | 5-7 minutes           |

| Input File Name             | Number of Edges | Matching Size |
| --------------------------- | --------------- | ------------- |
| soc-pokec-relationships.csv | 22,301,964      | 703,095       |
| soc-LiveJournal1.csv        | 42,851,237      | 1,890,074     |
| twitter_original_edges.csv  | 63,555,749      | 92,404        |
| com-orkut.ungraph.csv       | 117,185,083     | 1,461,419     |

After 2-3 repeated trials on each of the larger samples to test for robustness and precision of the output, it was concluded that the Israeli-Itai Algorithm had been implemented corrected. To improve the results, an attempt to identify and flip the augmenting paths was implemented.

As searching for augmenting paths are non-trivial, the initial approach was to fully abandon parallelization and implement an algorithm that searchs every augmenting path of a given length (n), by providing a scenario in which n edges are augmenting paths. In other words, the program was to iterate through the nodes and find a condition in which 0-1 are not matched, 1-2 are matched, 2-3 are not matched, and so on. However, through Profession Su's suggestion, it was deemed better to simultaneously search for some of the augmenting paths instead of iteratively finding all of them, as the prior algorithm had significantly faster runtime than the latter, and the advantages of finding "all" augmenting paths of length n was not beneficial enough relative to the computing power that it required.

Therefore, an alternative algorithm (augmenting_path_improver.scala) was implemented, searching for augmenting paths of length 3 at parallel at each iteration, and resolving conflicts when they occur. With the exception of twitter_original_edges.csv, each graph was run through the augmenting path algorithm once to record data, and then was run through multiple iterations until the improvements began to drastically reduce (less than 5,000 augmenting paths per iteration). In the case of twitter_original_edges.csv, due to the skewedness of the graph that led to vast numbers of conflicting augmenting paths that had to be stored and resolved, neither the local machine nor GCP was able to avoid running into Java Out of Memory Error.

## VI. Conclusion

There are several additional procedures that I would have carried out if I had more time and access to stronger computing power. The first is that I am unsure of the exact benefits of the fallback to the Greedy Random Matching Algorithm, as it came from a theoretical standpoint of observing later Israeli-Itai iteration results that showed very few matches or even no matches. Unlike my expectations, the 3-5 trials that I was able to carry out on the larger data (as smaller input would rarely have meaningful number of iterations to begin with), did not show noticeable differences in the total number of iterations nor the runtime.

The second part that I would like to study and investigate further regards the augmenting paths. Due to the nature of the assignment, my primary goal in implementing and testing out the augmenting path algorithm was the boost the matching size of the solutions, and therefore, the approach the I took was to mainly boost soc-LiveJournal1.csv and com-orkut.ungraph.csv, as they had the largest room for improvements relative to percent change. Unfortunately, what I was unable to address due to this approach was what exact 1/ε-approximation I am reaching through the iterations of augmenting path algorithm, and if, at a certain point, it would be/would have been more beneficial to improve my original algorithm and implement one that searches for augmenting paths of length 5 or more. If I had almost run out of augmenting paths of length 3 after a certain number of iterations, for a large and relatively well distributed data such as soc-pokec-relationships.csv, or soc-LiveJournal1.csv it could have been better to implement such an algorithm for a larger increase in the size of the matched set. 

## References
[1] Garrido, O., Jarominek, S., Lingas, A., Rytter, W. (1992). A simple randomized parallel algorithm for maximal f-matchings. In: Simon, I. (eds) LATIN '92. LATIN 1992. Lecture Notes in Computer Science, vol 583. Springer, Berlin, Heidelberg. https://doi.org/10.1007/BFb0023827 </br>
[2] Shang-En Huang and Hsin-Hao Su. 2023. (1-ϵ)-Approximate Maximum Weighted Matching in poly(1/ϵ, log n) Time in the Distributed and Parallel Settings. In Proceedings of the 2023 ACM Symposium on Principles of Distributed Computing (PODC '23). Association for Computing Machinery, New York, NY, USA, 44–54. https://doi.org/10.1145/3583668.3594570 </br>
[3] Leonid Barenboim, Michael Elkin, Seth Pettie, and Johannes Schneider. 2016. The Locality of Distributed Symmetry Breaking. J. ACM 63, 3, Article 20 (September 2016), 45 pages. https://doi.org/10.1145/2903137

## Result Replication
To replicate the results, each algorithm can be run in the following steps.  

Large Input (other csv files)
* Run "final_project.israeli-itai_matching" to produce initial matched set
* Run "final_project.augmenting_path_improver" with desired number of iterations to matched sets with improved size

The matching verifier accepts 2 file paths as arguments, the first being the path to the file containing the initial graph and the second being the path to the file containing the matching. It can be ran locally with the following command (keep in mind that your file paths may be different):
```
// Linux
spark-submit --master local[*] --class final_project.matching_verifier target/scala-2.12/project_3_2.12-1.0.jar /data/log_normal_100.csv data/log_normal_100_matching.csv

// Unix
spark-submit --master "local[*]" --class "final_project.matching_verifier" target/scala-2.12/project_3_2.12-1.0.jar data/log_normal_100.csv data/log_normal_100_matching.csv
```

### Correlation Clustering

The clustering verifier accepts 2 file paths as arguments, the first being the path to the file containing the initial graph and the second being the path to the file describing the clustering. It can be ran locally with the following command (keep in mind that your file paths may be different):
```
// Linux
spark-submit --master local[*] --class final_project.clustering_verifier target/scala-2.12/project_3_2.12-1.0.jar /data/log_normal_100.csv data/log_normal_100_clustering.csv

// Unix
spark-submit --master "local[*]" --class "final_project.clustering_verifier" target/scala-2.12/project_3_2.12-1.0.jar data/log_normal_100.csv data/log_normal_100_clustering.csv

```
