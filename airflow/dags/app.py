from datetime import timedelta
import json
from airflow import DAG
# Operators;
from airflow.models import Variable
from airflow.operators.http_operator import SimpleHttpOperator
from airflow.operators.python_operator import PythonOperator
from airflow.utils.dates import days_ago
from airflow.utils.helpers import chain
# These args will get passed on to each operator
# You can override them on a per-task basis during operator initialization
default_args = {
    'owner': 'Saffron',
    'depends_on_past': False,
    'start_date': days_ago(2),
    'email': ['example@example.com'],
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 1,
    'retry_delay': timedelta(minutes=500)
}
dag = DAG(
    'saffron_default_pipeline',
    default_args=default_args,
    description='A Saffron pipeline',
    schedule_interval=None,
)


message_dict = Variable.get("corpus", deserialize_json=True)
config_json = Variable.get("config", deserialize_json=True)

def extract_response(**kwargs):
    data = {}
    downstream_tasks = kwargs['ti'].task.downstream_task_ids
    if len(downstream_tasks) > 0:
        next_task = list(downstream_tasks)[0]
        document_list = kwargs['ti'].xcom_pull(task_ids='indexing_corpus')

        if next_task == "term_extraction":
            term_extraction_config = {'termExtraction': config_json['termExtraction']}
            data = {'config': term_extraction_config, "data": {"documents": json.loads(document_list)['documents']}}
        elif next_task == "author_extraction":
            author_extraction_config = {}
            data = {'config': author_extraction_config, "data": {"documents": json.loads(document_list)['documents']}}
        elif next_task == "author_connection":
            term_extraction_response_str = kwargs['ti'].xcom_pull(task_ids='term_extraction')
            author_connection_config = {'authorTerm': config_json['authorTerm']}
            data = {
                        'config': author_connection_config,
                        'data':
                            {  "documents": json.loads(document_list)['documents'],
                                'documentTermMapping': json.loads(term_extraction_response_str)['data']['documentTermMapping'],
                                "termsMapping": json.loads(term_extraction_response_str)['data']['termsMapping']
                            }
            }
        elif next_task == "term_similarity":
            term_extraction_response_str = kwargs['ti'].xcom_pull(task_ids='term_extraction')
            term_similarity_config = {'termSim': config_json['termSim']}
            data = {
                'config': term_similarity_config,
                'data':
                    {
                       'documentTermMapping': json.loads(term_extraction_response_str)['data']['documentTermMapping'],
                    }
            }
        elif next_task == "author_similarity":
            term_extraction_response_str = kwargs['ti'].xcom_pull(task_ids='term_extraction')
            term_similarity_config = {'termSim': config_json['termSim']}
            data = {
                'config': term_similarity_config,
                'data':
                    {
                        'documentTermMapping': json.loads(term_extraction_response_str)['data']['documentTermMapping'],
                    }
            }
        elif next_task == "taxonomy_extraction":
            term_extraction_response_str = kwargs['ti'].xcom_pull(task_ids='term_extraction')
            taxonomy_config = {'taxonomy': config_json['taxonomy']}
            data = {
                'config': taxonomy_config,
                'data':
                    {
                        'documentTermMapping': json.loads(term_extraction_response_str)['data']['documentTermMapping'],
                        "termsMapping": json.loads(term_extraction_response_str)['data']['termsMapping']
                    }
            }
        elif next_task == "kg_extraction":
            term_extraction_response_str = kwargs['ti'].xcom_pull(task_ids='term_extraction')
            data = {
                'config': {'taxonomy': config_json['taxonomy'], 'kg': config_json['kg']},
                'data': {
                    'documentTermMapping': json.loads(term_extraction_response_str)['data']['documentTermMapping'],
                    "termsMapping": json.loads(term_extraction_response_str)['data']['termsMapping']
                }
            }
    return json.dumps(data)




# Index Corpus
index_corpus = SimpleHttpOperator(
    task_id='indexing_corpus',
    endpoint='api/v1/documentindex',
    data=json.dumps(message_dict),
    headers={"Content-Type": "application/json"},
    xcom_push=True,
    extra_options={"timeout": 10},
    provide_context=True,
    response_check=lambda response: True if len(response.json()) != 0 else False,
    dag=dag
)

run_index_corpus_response_extractor = PythonOperator(
    task_id='run_index_corpus_response_extractor',
    provide_context=True,
    python_callable=extract_response,
    dag=dag
)


# Term Extraction
term_extraction = SimpleHttpOperator(
    task_id='term_extraction',
    endpoint='api/v1/term-extraction',
    http_conn_id='term_http_conn',
    data="{{ ti.xcom_pull(task_ids='run_index_corpus_response_extractor') }}",
    headers={"Content-Type": "application/json"},
    xcom_push=True,
    response_check=lambda response: True if len(response.json()) != 0 else False,
    dag=dag
)

run_term_extraction_response_extractor = PythonOperator(
    task_id='run_term_extraction_response_extractor',
    provide_context=True,
    python_callable=extract_response,
    dag=dag
)

# Author Extraction
author_extraction = SimpleHttpOperator(
    task_id='author_extraction',
    endpoint='api/v1/author-consolidation',
    http_conn_id='author_http_conn',
    data="{{ ti.xcom_pull(task_ids='run_term_extraction_response_extractor') }}",
    headers={"Content-Type": "application/json"},
    xcom_push=True,
    response_check=lambda response: True if len(response.json()) != 0 else False,
    dag=dag
)

run_author_extraction_response_extractor = PythonOperator(
    task_id='run_author_extraction_response_extractor',
    provide_context=True,
    python_callable=extract_response,
    dag=dag
)

# # Author Connection
author_connection = SimpleHttpOperator(
    task_id='author_connection',
    endpoint='api/v1/author-connection',
    http_conn_id='author_http_conn',
    data="{{ ti.xcom_pull(task_ids='run_author_extraction_response_extractor') }}",
    headers={"Content-Type": "application/json"},
    xcom_push=True,
    response_check=lambda response: True if len(response.json()) != 0 else False,
    dag=dag
)

run_author_connection_response_extractor = PythonOperator(
    task_id='run_author_connection_response_extractor',
    provide_context=True,
    python_callable=extract_response,
    dag=dag
)


#
# # Term Similarity
term_similarity = SimpleHttpOperator(
    task_id='term_similarity',
    endpoint='api/v1/term-similarity',
    http_conn_id='term_similarity_http_conn',
    data="{{ ti.xcom_pull(task_ids='run_author_connection_response_extractor') }}",
    headers={"Content-Type": "application/json"},
    xcom_push=True,
    response_check=lambda response: True if len(response.json()) != 0 else False,
    dag=dag
)

run_term_similarity_response_extractor = PythonOperator(
    task_id='run_term_similarity_response_extractor',
    provide_context=True,
    python_callable=extract_response,
    dag=dag
)

#
# # Author Similarity
author_similarity = SimpleHttpOperator(
    task_id='author_similarity',
    endpoint='api/v1/author-similarity',
    http_conn_id='author_http_conn',
    data="{{ ti.xcom_pull(task_ids='run_term_similarity_response_extractor') }}",
    headers={"Content-Type": "application/json"},
    xcom_push=True,
    response_check=lambda response: True if len(response.json()) == 0 else False,
    dag=dag
)

run_author_similarity_response_extractor = PythonOperator(
    task_id='run_author_similarity_response_extractor',
    provide_context=True,
    python_callable=extract_response,
    dag=dag
)

# # Taxonomy Extraction
taxonomy_extraction = SimpleHttpOperator(
    task_id='taxonomy_extraction',
    endpoint='api/v1/taxonomy-extraction',
    http_conn_id='taxonomy_http_conn',
    data="{{ ti.xcom_pull(task_ids='run_author_similarity_response_extractor') }}",
    headers={"Content-Type": "application/json"},
    xcom_push=True,
    response_check=lambda response: True if len(response.json()) != 0 else False,
    dag=dag
)

run_taxonomy_extraction_response_extractor = PythonOperator(
    task_id='run_taxonomy_extraction_response_extractor',
    provide_context=True,
    python_callable=extract_response,
    dag=dag
)

# # Knowledge Graph Extraction
kg_extraction = SimpleHttpOperator(
    task_id='kg_extraction',
    endpoint='api/v1/kg-extraction',
    http_conn_id='taxonomy_http_conn',
    data="{{ ti.xcom_pull(task_ids='run_taxonomy_extraction_response_extractor') }}",
    headers={"Content-Type": "application/json"},
    xcom_push=True,
    response_check=lambda response: True if len(response.json()) == 0 else False,
    retries=0,
    execution_timeout=timedelta(hours=3),
    dag=dag
)

run_kg_extraction_response_extractor = PythonOperator(
    task_id='run_kg_extraction_response_extractor',
    provide_context=True,
    python_callable=extract_response,
    dag=dag
)

create_tasks = [
    index_corpus,
    run_index_corpus_response_extractor,
    term_extraction,
    run_term_extraction_response_extractor,
    author_extraction,
    run_author_extraction_response_extractor,
    author_connection,
    run_author_connection_response_extractor,
    term_similarity,
    run_term_similarity_response_extractor,
    author_similarity,
    run_author_similarity_response_extractor,
    taxonomy_extraction,
    run_taxonomy_extraction_response_extractor,
    kg_extraction,
    run_kg_extraction_response_extractor
]
#
chain(*create_tasks)
