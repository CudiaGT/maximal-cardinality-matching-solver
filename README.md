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
As the goal to design and implement an algorithm that can efficiently create a maximal matched set had been accomplished, the next step was to improve the results (enlarging the size of the matched set) by searching and converting augmenting paths. The presented algorithm stored in "augmenting_path_improver.scala" searches for augmenting paths of length 3 at parallel in each iteration, and resolves any conflict through randomly assigning hash values to the found augmenting paths and incorporating them in order of priority.

The approximation bound from augmenting path length states that when the shortest augmenting paths have length &ge; 2𝑘−1, then the current matching is at least (1−1/𝑘)⋅𝑂𝑃𝑇. In other words, by implementing this algorithm, the resulting matched set will no longer have augmenting paths of length 3, making the shortest augmenting paths to have the minimum length of 5 edges. Therefore, a successful elimination of augmenting paths with length 3 will guarantee a 2/3-optimal size of the resulting matched set.

## V. Augmenting Path Enhancement Results & Analysis
### Analysis of Theoretical Runtime
| Algorithm              | Time-Complexity  | Space-Complexity | Scalability            | Parellelization |
| ---------------------- | ---------------- | ---------------- | ---------------------- | --------------- |
| Augmenting Path        | O(E + plog(p)    | O(m + n + p)     | Yes                    | Yes             |

### Results after Single Execution
| File Name                   | Original Matching | After 1 Iteration | After (n) Iterations | Runtime per Iteration |
| --------------------------- | ----------------- | ----------------- | -------------------- | --------------------- |
| soc-pokec-relationships.csv | 599,530           | 623,483           | 703,095 (16)         | 1-2 minutes           |
| soc-LiveJournal1            | 1,578,566         | 1,692,282         | 1,890,074 (10)       | 2-4 minutes           |
| twitter_original_edges      | 92,404            | N/A               | N/A                  | N/A                   |
| com-orkut.ungraph.csv       | 1,339,741         | 1,363,212         | 1,461,419 (10)       | 5-7 minutes           |

### Results after Iterative Executions
| Input File Name             | Number of Edges | Originial Matching Size | Improved Matching Size | % Change of Size |
| --------------------------- | --------------- | ----------------------- | ---------------------- | ---------------- |
| soc-pokec-relationships.csv | 22,301,964      | 599,530                 | 703,095                | 17.27% Increase  |
| soc-LiveJournal1.csv        | 42,851,237      | 1,578,566               | 1,890,074              | 19.73% Increase  |
| twitter_original_edges.csv  | 63,555,749      | 92,404                  | 92,404                 | No Change        |
| com-orkut.ungraph.csv       | 117,185,083     | 1,339,741               | 1,461,419              | 9.08% Increase   |

###

The enhancement algorithm was able to increase the matching size by 9-20 percent for most of the inputs. Nevertheless, the algorithm faced challenges in searching for augmenting paths in extremely skewed data such as the twitter_original_edges.csv, where it repeated ran into Java OutOfMemoryError, exceeding the 12 gigabytes of memory that was assigned to the execution of this program. Finally, although it does not relate to the limitations of the algorithm itself, but due to the nature of maximal matching problems and graph theory, it can also be seen that the auxiliary algorithm was more effective in increasing the matching size when the edges are relatively evenly distributed (soc-pokec-relationships.csv and soc-LiveJournal1.csv), than when the graph was overly dense or skewed (twitter_original_edges.csv and com-orkut.ungraph.csv).

## VI. Conclusion
In conlcusion, this project was able to achieve two meaningful results: 1) designing and implementing a hybrid algorithm that utilizes Israeli-Itai algorithm for scalability and Greedy Random Matching algorithm for reducing unnecessary iterations, and 2) ensuring a 2/3-optimal size for the matched set by implementing an algorithm that searches and converts augmenting paths of length 3.

For future improvements, the focus would on empirical analysis of the benefits of fallback to Greedy Random Matching algorithm, providing numbers that can show its advantages compared to simply utilizing the Israeli-Itai algorithm. Secondly, implementation of dynamic component to the augmenting path enhancement algorithm, allowing the user to select the length of the augmenting path in which they would like to search and convert, providing the user with the freedom to select how much runtime they are willing to give up in return of the larger resulting matched set.

## References
[1] Garrido, O., Jarominek, S., Lingas, A., Rytter, W. (1992). A simple randomized parallel algorithm for maximal f-matchings. In: Simon, I. (eds) LATIN '92. LATIN 1992. Lecture Notes in Computer Science, vol 583. Springer, Berlin, Heidelberg. https://doi.org/10.1007/BFb0023827 </br>
[2] Shang-En Huang and Hsin-Hao Su. 2023. (1-ϵ)-Approximate Maximum Weighted Matching in poly(1/ϵ, log n) Time in the Distributed and Parallel Settings. In Proceedings of the 2023 ACM Symposium on Principles of Distributed Computing (PODC '23). Association for Computing Machinery, New York, NY, USA, 44–54. https://doi.org/10.1145/3583668.3594570 </br>
[3] Leonid Barenboim, Michael Elkin, Seth Pettie, and Johannes Schneider. 2016. The Locality of Distributed Symmetry Breaking. J. ACM 63, 3, Article 20 (September 2016), 45 pages. https://doi.org/10.1145/2903137

## Result Replication
To replicate the results, each algorithm can be run in the following steps.

The matching verifier accepts 2 file paths as arguments, the first being the path to the file containing the initial graph and the second being the path to the file containing the matching. It can be ran locally with the following command (keep in mind that your file paths may be different):

* Run "final_project.israeli-itai_matching" to produce initial matched set
```
// Linux
spark-submit --master local[*] --class final_project.matching_verifier target/scala-2.12/project_3_2.12-1.0.jar /data/log_normal_100.csv data/log_normal_100_matching.csv

// Unix
spark-submit --master "local[*]" --class "final_project.matching_verifier" target/scala-2.12/project_3_2.12-1.0.jar data/log_normal_100.csv data/log_normal_100_matching.csv
```
* Run "final_project.augmenting_path_improver" with desired number of iterations to matched sets with improved size
```
// Linux
spark-submit --master local[*] --class final_project.matching_verifier target/scala-2.12/project_3_2.12-1.0.jar /data/log_normal_100.csv data/log_normal_100_matching.csv

// Unix
spark-submit --master "local[*]" --class "final_project.matching_verifier" target/scala-2.12/project_3_2.12-1.0.jar data/log_normal_100.csv data/log_normal_100_matching.csv
```
