from fastapi import FastAPI
from pydantic import BaseModel
from transformers import AutoModelForSequenceClassification, AutoTokenizer
import torch

app = FastAPI()

MODEL_NAME = "BAAI/bge-reranker-v2-m3"
tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
model = AutoModelForSequenceClassification.from_pretrained(MODEL_NAME)
model.eval()


class RerankRequest(BaseModel):
    model: str = MODEL_NAME
    query: str
    documents: list[str]


class RerankResult(BaseModel):
    index: int
    relevance_score: float


class RerankResponse(BaseModel):
    results: list[RerankResult]
    model: str


@app.post("/v1/rerank", response_model=RerankResponse)
async def rerank(req: RerankRequest):
    pairs = [[req.query, doc] for doc in req.documents]
    inputs = tokenizer(pairs, padding=True, truncation=True, max_length=512, return_tensors="pt")

    with torch.no_grad():
        scores = model(**inputs).logits.squeeze(-1)

    if scores.dim() == 0:
        scores = scores.unsqueeze(0)

    results = []
    for i, score in enumerate(scores.tolist()):
        results.append(RerankResult(index=i, relevance_score=score))

    return RerankResponse(results=results, model=req.model)
